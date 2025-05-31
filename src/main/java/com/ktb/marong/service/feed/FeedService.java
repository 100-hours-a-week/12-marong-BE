package com.ktb.marong.service.feed;

import com.ktb.marong.common.util.WeekCalculator;
import com.ktb.marong.domain.feed.Post;
import com.ktb.marong.domain.feed.PostLike;
import com.ktb.marong.domain.group.UserGroup;
import com.ktb.marong.domain.manitto.Manitto;
import com.ktb.marong.domain.mission.Mission;
import com.ktb.marong.domain.mission.UserMission;
import com.ktb.marong.domain.user.User;
import com.ktb.marong.dto.request.feed.PostLikeRequestDto;
import com.ktb.marong.dto.request.feed.PostRequestDto;
import com.ktb.marong.dto.response.feed.PostLikeResponseDto;
import com.ktb.marong.dto.response.feed.PostPageResponseDto;
import com.ktb.marong.dto.response.feed.PostResponseDto;
import com.ktb.marong.exception.CustomException;
import com.ktb.marong.exception.ErrorCode;
import com.ktb.marong.repository.*;
import com.ktb.marong.service.file.FileUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserRepository userRepository;
    private final MissionRepository missionRepository;
    private final AnonymousNameRepository anonymousNameRepository;
    private final UserMissionRepository userMissionRepository;
    private final ManittoRepository manittoRepository;
    private final UserGroupRepository userGroupRepository;
    private final FileUploadService fileUploadService;

    /**
     * 게시글 업로드
     */
    @Transactional
    public Long savePost(Long userId, Long groupId, PostRequestDto requestDto, MultipartFile image) {
        log.info("게시글 저장 시작: userId={}, groupId={}, missionId={}", userId, groupId, requestDto.getMissionId());

        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. 사용자가 해당 그룹에 속해있는지 확인
        UserGroup userGroup = userGroupRepository.findByUserIdAndGroupId(userId, groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND,
                        "해당 그룹에 속하지 않은 사용자입니다."));

        // 3. 그룹 내 닉네임 설정 여부 확인
        if (!userGroup.hasGroupUserNickname()) {
            throw new CustomException(ErrorCode.GROUP_NICKNAME_REQUIRED,
                    "그룹 내 닉네임을 먼저 설정해주세요.");
        }

        // 4. 미션 조회
        Mission mission = missionRepository.findById(requestDto.getMissionId())
                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_NOT_FOUND));

        // 5. 현재 주차 계산
        int currentWeek = WeekCalculator.getCurrentWeek();

        // 6. 미션이 현재 사용자에게 해당 그룹에서 할당된 것인지 확인
        UserMission userMission = userMissionRepository.findByUserIdAndGroupIdAndMissionIdAndWeek(
                        userId, groupId, requestDto.getMissionId(), currentWeek)
                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_NOT_ASSIGNED,
                        "해당 그룹에서 할당되지 않은 미션입니다."));

        // 7. 미션 상태가 진행 중인지 확인
        if (!"ing".equals(userMission.getStatus())) {
            throw new CustomException(ErrorCode.MISSION_ALREADY_COMPLETED, "이미 완료된 미션입니다.");
        }

        // 8. 해당 미션을 현재 주차에 이미 수행했는지 확인
        int postCount = postRepository.countByUserIdAndMissionIdAndWeekAndGroupId(
                userId, requestDto.getMissionId(), currentWeek, groupId);

        if (postCount > 0) {
            throw new CustomException(ErrorCode.MISSION_ALREADY_COMPLETED,
                    "이번 주차에 해당 그룹에서 이미 해당 미션을 완료했습니다.");
        }

        // 9. 현재 사용자가 해당 그룹에서 마니또인지 확인 및 마니띠 정보 조회
        List<Manitto> manittoList = manittoRepository.findByManittoIdAndGroupIdAndWeek(userId, groupId, currentWeek);
        if (manittoList.isEmpty()) {
            throw new CustomException(ErrorCode.MANITTO_NOT_FOUND,
                    "해당 그룹에서 마니또 매칭이 되지 않았습니다.");
        }

        Manitto manitto = manittoList.get(0);
        String manitteeName = manitto.getManittee().getNickname(); // 마니띠(대상자)의 실명

        // 10. 익명 이름 조회 (그룹별 익명 이름)
        String anonymousName = anonymousNameRepository.findAnonymousNameByUserIdAndGroupIdAndWeek(
                        userId, groupId, currentWeek)
                .orElse("익명의 " + getRandomAnimal()); // 없으면 기본 이름 생성

        // 11. 이미지 업로드 처리
        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            try {
                imageUrl = fileUploadService.uploadFile(image, "feeds");
            } catch (IOException e) {
                log.error("이미지 업로드 실패: {}", e.getMessage());
                throw new CustomException(ErrorCode.FILE_UPLOAD_ERROR);
            }
        }

        // 12. 게시글 생성
        Post post = Post.builder()
                .user(user)
                .groupId(groupId)
                .mission(mission)
                .week(currentWeek)
                .anonymousSnapshotName(anonymousName)
                .manitteeName(manitteeName)
                .content(requestDto.getContent())
                .imageUrl(imageUrl)
                .build();

        // 13. 게시글 저장
        Post savedPost = postRepository.save(post);

        // 14. 미션 완료 상태 업데이트
        updateMissionStatus(userId, groupId, mission.getId(), currentWeek);

        log.info("게시글 저장 완료: postId={}, userId={}, groupId={}", savedPost.getId(), userId, groupId);
        return savedPost.getId();
    }

    /**
     * 게시글 좋아요 등록/취소
     */
    @Transactional
    public PostLikeResponseDto toggleLike(Long userId, Long feedId, PostLikeRequestDto requestDto) {
        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. 게시글 조회
        Post post = postRepository.findById(feedId)
                .orElseThrow(() -> new CustomException(ErrorCode.FEED_NOT_FOUND));

        // 3. 사용자가 해당 게시글의 그룹에 속해있는지 확인
        if (!userGroupRepository.existsByUserIdAndGroupId(userId, post.getGroupId())) {
            throw new CustomException(ErrorCode.GROUP_NOT_FOUND,
                    "해당 게시글의 그룹에 속하지 않은 사용자입니다.");
        }

        boolean isCancel = requestDto.getCancel();

        if (isCancel) {
            // 좋아요 취소
            PostLike postLike = postLikeRepository.findByUserAndPost(user, post)
                    .orElseThrow(() -> new CustomException(ErrorCode.NOT_LIKED));

            postLikeRepository.delete(postLike);
            log.info("좋아요 취소 처리: userId={}, feedId={}", userId, feedId);
        } else {
            // 좋아요 등록
            if (postLikeRepository.existsByUserAndPost(user, post)) {
                throw new CustomException(ErrorCode.ALREADY_LIKED);
            }

            PostLike postLike = PostLike.builder()
                    .user(user)
                    .post(post)
                    .build();

            postLikeRepository.save(postLike);
            log.info("좋아요 등록 처리: userId={}, feedId={}", userId, feedId);
        }

        // 현재 게시글의 좋아요 수 조회
        int likeCount = postLikeRepository.countByPostId(feedId);

        return new PostLikeResponseDto(likeCount);
    }

    /**
     * 게시글 목록 조회 (그룹별 분리)
     */
    @Transactional(readOnly = true)
    public PostPageResponseDto getPosts(Long userId, Long groupId, int page, int pageSize) {
        log.info("게시글 목록 조회: userId={}, groupId={}, page={}", userId, groupId, page);

        // 1. 사용자가 해당 그룹에 속해있는지 확인
        if (!userGroupRepository.existsByUserIdAndGroupId(userId, groupId)) {
            throw new CustomException(ErrorCode.GROUP_NOT_FOUND,
                    "해당 그룹에 속하지 않은 사용자입니다.");
        }

        // 2. 페이지네이션 설정
        Pageable pageable = PageRequest.of(page - 1, pageSize);

        // 3. 특정 그룹의 게시글만 조회
        Page<Post> postPage = postRepository.findAllByGroupIdOrderByCreatedAtDesc(groupId, pageable);

        // 4. DTO 변환
        List<PostResponseDto> postDtos = postPage.getContent().stream()
                .map(post -> {
                    int likeCount = postLikeRepository.countByPostId(post.getId());
                    boolean isLiked = postLikeRepository.existsByUserAndPost(
                            userRepository.findById(userId)
                                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND)),
                            post);
                    return PostResponseDto.fromEntity(post, likeCount, isLiked);
                })
                .collect(Collectors.toList());

        log.info("게시글 목록 조회 완료: groupId={}, totalElements={}", groupId, postPage.getTotalElements());

        return PostPageResponseDto.builder()
                .page(page)
                .pageSize(pageSize)
                .totalFeeds((int) postPage.getTotalElements())
                .feeds(postDtos)
                .build();
    }

    /**
     * 사용자의 기본으로 선택될 그룹 ID 조회 (가장 최근 가입한 그룹) -> 로그인 후 처음 로딩될 그룹의 피드
     */
    @Transactional(readOnly = true)
    public Long getDefaultGroupId(Long userId) {
        log.info("기본 그룹 ID 조회: userId={}", userId);

        List<UserGroup> userGroups = userGroupRepository.findByUserIdWithGroup(userId);

        if (userGroups.isEmpty()) {
            throw new CustomException(ErrorCode.GROUP_NOT_FOUND, "가입된 그룹이 없습니다.");
        }

        // 가장 최근에 가입한 그룹 찾기 (joinedAt 기준 내림차순 정렬 후 첫 번째)
        UserGroup latestGroup = userGroups.stream()
                .max((g1, g2) -> g1.getJoinedAt().compareTo(g2.getJoinedAt()))
                .orElse(userGroups.get(0)); // 혹시 모를 상황 대비

        Long defaultGroupId = latestGroup.getGroup().getId();
        log.info("기본 그룹 ID 결정: userId={}, defaultGroupId={}, groupName={}",
                userId, defaultGroupId, latestGroup.getGroup().getName());

        return defaultGroupId;
    }

    /**
     * 그룹별 게시글 통계 정보 조회
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getFeedStats(Long userId, Long groupId) {
        log.info("피드 통계 조회: userId={}, groupId={}", userId, groupId);

        // 1. 사용자가 해당 그룹에 속해있는지 확인
        if (!userGroupRepository.existsByUserIdAndGroupId(userId, groupId)) {
            throw new CustomException(ErrorCode.GROUP_NOT_FOUND,
                    "해당 그룹에 속하지 않은 사용자입니다.");
        }

        // 2. 현재 주차 정보
        int currentWeek = WeekCalculator.getCurrentWeek();

        // 3. 그룹 내 전체 게시글 수
        Page<Post> allPosts = postRepository.findAllByGroupIdOrderByCreatedAtDesc(
                groupId, PageRequest.of(0, 1));
        long totalPosts = allPosts.getTotalElements();

        // 4. 현재 주차 게시글 수
        Page<Post> weeklyPosts = postRepository.findAllByGroupIdAndWeekOrderByCreatedAtDesc(
                groupId, currentWeek, PageRequest.of(0, 1));
        long weeklyPostCount = weeklyPosts.getTotalElements();

        // 5. 그룹 멤버 수
        int memberCount = userGroupRepository.countByGroupId(groupId);

        // 6. 내가 작성한 게시글 수 (해당 그룹에서)
        long myPostsCount = postRepository.findAllByGroupIdOrderByCreatedAtDesc(groupId, PageRequest.of(0, Integer.MAX_VALUE))
                .getContent()
                .stream()
                .filter(post -> post.getUser().getId().equals(userId))
                .count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalPosts", totalPosts);
        stats.put("weeklyPosts", weeklyPostCount);
        stats.put("memberCount", memberCount);
        stats.put("myPosts", myPostsCount);
        stats.put("currentWeek", currentWeek);

        log.info("피드 통계 조회 완료: groupId={}, stats={}", groupId, stats);
        return stats;
    }

    /**
     * 미션 완료 상태 업데이트
     */
    private void updateMissionStatus(Long userId, Long groupId, Long missionId, Integer week) {
        userMissionRepository.findByUserIdAndGroupIdAndMissionIdAndWeek(userId, groupId, missionId, week)
                .ifPresent(userMission -> {
                    userMission.complete();
                    userMissionRepository.save(userMission);
                    log.info("미션 완료 상태 업데이트: userId={}, groupId={}, missionId={}, week={}",
                            userId, groupId, missionId, week);
                });
    }

    /**
     * 랜덤 동물 이름 생성
     */
    private String getRandomAnimal() {
        String[] animals = {"판다", "고양이", "강아지", "호랑이", "코끼리", "원숭이", "토끼",
                "사자", "여우", "늑대", "곰", "펭귄", "부엉이", "독수리"};
        return animals[new Random().nextInt(animals.length)];
    }
}