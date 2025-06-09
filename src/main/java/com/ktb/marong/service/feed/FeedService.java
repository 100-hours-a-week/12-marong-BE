package com.ktb.marong.service.feed;

import com.ktb.marong.common.util.WeekCalculator;
import com.ktb.marong.domain.feed.Post;
import com.ktb.marong.domain.feed.PostLike;
import com.ktb.marong.domain.group.Group;
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
import java.time.DayOfWeek;
import java.time.LocalDateTime;
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
    private final GroupRepository groupRepository;
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

        // 2. 그룹 존재 여부 확인
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND));

        // 3. 사용자가 해당 그룹에 속해있는지 확인
        UserGroup userGroup = userGroupRepository.findByUserIdAndGroupId(userId, groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND,
                        "해당 그룹에 속하지 않은 사용자입니다."));

        // 4. 그룹 내 닉네임 설정 여부 확인
        if (!userGroup.hasGroupUserNickname()) {
            throw new CustomException(ErrorCode.GROUP_NICKNAME_REQUIRED,
                    "그룹 내 닉네임을 먼저 설정해주세요.");
        }

        // 5. 미션 조회
        Mission mission = missionRepository.findById(requestDto.getMissionId())
                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_NOT_FOUND));

        // 6. 현재 주차 계산
        int currentWeek = WeekCalculator.getCurrentWeek();

        // 7. 미션이 현재 사용자에게 해당 그룹에서 할당된 것인지 확인
        UserMission userMission = userMissionRepository.findByUserIdAndGroupIdAndMissionIdAndWeek(
                        userId, groupId, requestDto.getMissionId(), currentWeek)
                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_NOT_ASSIGNED,
                        "해당 그룹에서 할당되지 않은 미션입니다."));

        // 8. 미션 상태가 진행 중인지 확인
        if (!"ing".equals(userMission.getStatus())) {
            throw new CustomException(ErrorCode.MISSION_ALREADY_COMPLETED, "이미 완료된 미션입니다.");
        }

        // 9. 해당 미션을 현재 주차에 이미 수행했는지 확인
        int postCount = postRepository.countByUserIdAndMissionIdAndWeekAndGroupId(
                userId, requestDto.getMissionId(), currentWeek, groupId);

        if (postCount > 0) {
            throw new CustomException(ErrorCode.MISSION_ALREADY_COMPLETED,
                    "이번 주차에 해당 그룹에서 이미 해당 미션을 완료했습니다.");
        }

        // 10. 현재 사용자가 해당 그룹에서 마니또인지 확인 및 마니띠 정보 조회
        List<Manitto> manittoList = manittoRepository.findByManittoIdAndGroupIdAndWeek(userId, groupId, currentWeek);
        if (manittoList.isEmpty()) {
            throw new CustomException(ErrorCode.MANITTO_NOT_FOUND,
                    "해당 그룹에서 마니또 매칭이 되지 않았습니다.");
        }

        Manitto manitto = manittoList.get(0);
        User manitteeUser = manitto.getManittee(); // 마니띠 사용자 객체

        // 마니띠의 그룹 내 닉네임 설정 여부 확인
        UserGroup manitteeUserGroup = userGroupRepository.findByUserIdAndGroupId(manitteeUser.getId(), groupId)
                .orElse(null);

        // 마니띠 이름 결정: 그룹 내 닉네임이 있으면 그걸로 사용, 없으면 카카오 실명으로 사용
        String manitteeName;
        if (manitteeUserGroup != null && manitteeUserGroup.hasGroupUserNickname()) {
            manitteeName = manitteeUserGroup.getGroupUserNickname(); // 그룹 내 닉네임
            log.info("마니띠 그룹 내 닉네임 사용: manitteeId={}, groupNickname={}",
                    manitteeUser.getId(), manitteeName);
        } else {
            manitteeName = manitteeUser.getNickname(); // 카카오 실명
            log.info("마니띠 카카오 실명 사용: manitteeId={}, kakaoName={}",
                    manitteeUser.getId(), manitteeName);
        }

        // 11. 익명 이름 조회 (그룹별 익명 이름)
        String anonymousName = anonymousNameRepository.findAnonymousNameByUserIdAndGroupIdAndWeek(
                        userId, groupId, currentWeek)
                .orElse("익명의 " + getRandomAnimal()); // 없으면 기본 이름 생성

        // 12. 이미지 업로드 처리
        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            try {
                imageUrl = fileUploadService.uploadFile(image, "feeds");
            } catch (IOException e) {
                log.error("이미지 업로드 실패: {}", e.getMessage());
                throw new CustomException(ErrorCode.FILE_UPLOAD_ERROR);
            }
        }

        // 13. 게시글 생성
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

        // 14. 게시글 저장
        Post savedPost = postRepository.save(post);

        // 15. 미션 완료 상태 업데이트
        updateMissionStatus(userId, groupId, mission.getId(), currentWeek);

        log.info("게시글 저장 완료: postId={}, userId={}, groupId={}, manitteeName={}",
                savedPost.getId(), userId, groupId, manitteeName);
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

        // 1. 그룹 존재 여부 확인
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND));

        // 2. 사용자가 해당 그룹에 속해있는지 확인하고 그룹 정보도 함께 조회
        UserGroup userGroup = userGroupRepository.findByUserIdAndGroupId(userId, groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND,
                        "해당 그룹에 속하지 않은 사용자입니다."));

        // 2. 페이지네이션 설정
        Pageable pageable = PageRequest.of(page - 1, pageSize);

        // 3. 특정 그룹의 게시글만 조회
        Page<Post> postPage = postRepository.findAllByGroupIdOrderByCreatedAtDesc(groupId, pageable);

        // 4. 현재 주차 및 마니또 공개 시점 확인
        int currentWeek = WeekCalculator.getCurrentWeek();
        boolean isManittoRevealTime = isManittoRevealTime();

        // 5. DTO 변환
        List<PostResponseDto> postDtos = postPage.getContent().stream()
                .map(post -> {
                    int likeCount = postLikeRepository.countByPostId(post.getId());
                    boolean isLiked = postLikeRepository.existsByUserAndPost(
                            userRepository.findById(userId)
                                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND)),
                            post);

                    // 실시간 마니띠 이름 결정
                    String realTimeManitteeName = determineManitteeNameForPost(post, groupId);

                    // 게시글 작성자 이름 결정
                    String authorName = determineAuthorNameForPost(post, groupId, currentWeek, isManittoRevealTime);

                    return PostResponseDto.fromEntityWithRealTimeManitteeNameAndAuthor(
                            post, likeCount, isLiked, realTimeManitteeName, authorName);
                })
                .collect(Collectors.toList());

        log.info("게시글 목록 조회 완료: groupId={}, groupName={}, totalElements={}",
                groupId, group.getName(), postPage.getTotalElements());

        // 6. 그룹 정보 포함하여 응답 생성
        return PostPageResponseDto.builder()
                .page(page)
                .pageSize(pageSize)
                .totalFeeds((int) postPage.getTotalElements())
                .groupId(groupId)
                .groupName(group.getName())
                .feeds(postDtos)
                .build();
    }

    /**
     * 게시글 작성자 이름 결정
     * 규칙:
     * 1. 현재 주차 게시글 + 마니또 공개 시점 이전 = 익명 이름만
     * 2. 현재 주차 게시글 + 마니또 공개 시점 이후 = "그룹닉네임 (익명이름)" 형태
     * 3. 지난 주차 게시글 = 항상 "그룹닉네임 (익명이름)" 형태 (해당 주차 공개 시점 이후)
     */
    private String determineAuthorNameForPost(Post post, Long groupId, int currentWeek, boolean isManittoRevealTime) {
        try {
            int postWeek = post.getWeek();
            String anonymousName = post.getAnonymousSnapshotName();

            // 케이스 1: 현재 주차 게시글이면서 마니또 공개 시점 이전
            if (postWeek == currentWeek && !isManittoRevealTime) {
                log.debug("현재 주차 비공개 시점 게시글: postId={}, week={}, 익명이름만 표시",
                        post.getId(), postWeek);
                return anonymousName;
            }

            // 케이스 2: 현재 주차 게시글이면서 마니또 공개 시점 이후
            // 케이스 3: 지난 주차 게시글 (이미 해당 주차의 공개 시점이 지남)
            if ((postWeek == currentWeek && isManittoRevealTime) || postWeek < currentWeek) {
                // 게시글 작성자의 그룹 내 정보 조회
                User postAuthor = post.getUser();
                UserGroup authorUserGroup = userGroupRepository.findByUserIdAndGroupId(
                        postAuthor.getId(), groupId).orElse(null);

                String displayName;
                if (authorUserGroup != null && authorUserGroup.hasGroupUserNickname()) {
                    // 그룹 내 닉네임이 있는 경우
                    displayName = authorUserGroup.getGroupUserNickname();
                    log.debug("그룹 내 닉네임 사용: userId={}, groupNickname={}",
                            postAuthor.getId(), displayName);
                } else {
                    // 그룹 내 닉네임이 없는 경우 카카오 실명 사용
                    displayName = postAuthor.getNickname();
                    log.debug("카카오 실명 사용: userId={}, kakaoName={}",
                            postAuthor.getId(), displayName);
                }

                // "그룹닉네임 (익명이름)" 또는 "카카오실명 (익명이름)" 형태로 반환
                String finalAuthorName = displayName + " (" + anonymousName + ")";

                String caseDescription = (postWeek == currentWeek) ? "현재 주차 공개 시점" : "지난 주차";
                log.debug("{} 게시글 작성자 이름 변경: postId={}, week={}, originalName={}, newName={}",
                        caseDescription, post.getId(), postWeek, anonymousName, finalAuthorName);

                return finalAuthorName;
            }

            // 케이스 4: 미래 주차 게시글 (일반적으로 발생하지 않아야 함)
            log.warn("미래 주차 게시글 발견: postId={}, postWeek={}, currentWeek={}",
                    post.getId(), postWeek, currentWeek);
            return anonymousName;

        } catch (Exception e) {
            log.warn("게시글 작성자 이름 결정 실패, 기존 이름 사용: postId={}, error={}",
                    post.getId(), e.getMessage());
            // 오류 발생시 기존 익명 이름 사용
            return post.getAnonymousSnapshotName();
        }
    }

    /**
     * 현재 시간이 마니또 공개 시점인지 확인
     * 마니또 공개 시점: 금요일 오후 5시 이후 ~ 다음 주 월요일 오후 12시 이전
     */
    private boolean isManittoRevealTime() {
        LocalDateTime now = LocalDateTime.now();
        DayOfWeek dayOfWeek = now.getDayOfWeek();
        int hour = now.getHour();

        // 금요일 17시 이후인 경우
        if (dayOfWeek == DayOfWeek.FRIDAY && hour >= 17) {
            return true;
        }
        // 토요일, 일요일인 경우
        else if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return true;
        }
        // 월요일 12시 이전인 경우
        else if (dayOfWeek == DayOfWeek.MONDAY && hour < 12) {
            return true;
        }

        return false;
    }

    /**
     * 게시글의 마니띠 이름을 실시간으로 결정
     */
    private String determineManitteeNameForPost(Post post, Long groupId) {
        try {
            // 해당 게시글 작성자의 해당 주차 마니또 매칭 정보 조회
            List<Manitto> manittoList = manittoRepository.findByManittoIdAndGroupIdAndWeek(
                    post.getUser().getId(), groupId, post.getWeek());

            if (manittoList.isEmpty()) {
                // 매칭 정보가 없으면 기존 DB에 저장된 이름 사용
                return post.getManitteeName();
            }

            Manitto manitto = manittoList.get(0);
            User manitteeUser = manitto.getManittee();

            // 마니띠의 현재 그룹 내 닉네임 설정 여부 확인
            UserGroup manitteeUserGroup = userGroupRepository.findByUserIdAndGroupId(
                    manitteeUser.getId(), groupId).orElse(null);

            // 그룹 내 닉네임이 있으면 그걸로 사용, 없으면 카카오 실명으로 사용
            if (manitteeUserGroup != null && manitteeUserGroup.hasGroupUserNickname()) {
                return manitteeUserGroup.getGroupUserNickname(); // 그룹 내 닉네임
            } else {
                return manitteeUser.getNickname(); // 카카오 실명
            }

        } catch (Exception e) {
            log.warn("마니띠 이름 실시간 결정 실패, 기존 이름 사용: postId={}, error={}",
                    post.getId(), e.getMessage());
            // 오류 발생시 기존 DB에 저장된 이름 사용
            return post.getManitteeName();
        }
    }

    /**
     * 사용자의 기본으로 선택될 그룹 ID 조회 (가장 최근 가입한 그룹) -> 로그인 후 처음 로딩될 그룹의 피드
     * 신규 사용자(그룹 미가입)의 경우 null 반환
     */
    @Transactional(readOnly = true)
    public Long getDefaultGroupId(Long userId) {
        log.info("기본 그룹 ID 조회: userId={}", userId);

        List<UserGroup> userGroups = userGroupRepository.findByUserIdWithGroup(userId);

        if (userGroups.isEmpty()) {
            log.info("신규 사용자 - 가입된 그룹이 없음: userId={}", userId);
            return null; // 에러 대신 null 반환
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
     * 그룹별 게시글 통계 정보 조회 (신규 사용자 안전 처리)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getFeedStats(Long userId, Long groupId) {
        log.info("피드 통계 조회: userId={}, groupId={}", userId, groupId);

        // 신규 사용자인 경우 (groupId가 null)
        if (groupId == null) {
            log.info("신규 사용자 피드 통계 요청: userId={}", userId);
            Map<String, Object> emptyStats = new HashMap<>();
            emptyStats.put("totalPosts", 0);
            emptyStats.put("weeklyPosts", 0);
            emptyStats.put("memberCount", 0);
            emptyStats.put("myPosts", 0);
            emptyStats.put("currentWeek", WeekCalculator.getCurrentWeek());
            emptyStats.put("isNewUser", true); // 신규 사용자 플래그
            return emptyStats;
        }

        // 1. 그룹 존재 여부 확인
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND));

        // 2. 사용자가 해당 그룹에 속해있는지 확인
        if (!userGroupRepository.existsByUserIdAndGroupId(userId, groupId)) {
            throw new CustomException(ErrorCode.GROUP_NOT_FOUND,
                    "해당 그룹에 속하지 않은 사용자입니다.");
        }

        // 3. 현재 주차 정보
        int currentWeek = WeekCalculator.getCurrentWeek();

        // 4. 그룹 내 전체 게시글 수
        Page<Post> allPosts = postRepository.findAllByGroupIdOrderByCreatedAtDesc(
                groupId, PageRequest.of(0, 1));
        long totalPosts = allPosts.getTotalElements();

        // 5. 현재 주차 게시글 수
        Page<Post> weeklyPosts = postRepository.findAllByGroupIdAndWeekOrderByCreatedAtDesc(
                groupId, currentWeek, PageRequest.of(0, 1));
        long weeklyPostCount = weeklyPosts.getTotalElements();

        // 6. 그룹 멤버 수
        int memberCount = userGroupRepository.countByGroupId(groupId);

        // 7. 내가 작성한 게시글 수 (해당 그룹에서)
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
        stats.put("isNewUser", false); // 기존 사용자

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