package com.ktb.marong.service.feed;

import com.ktb.marong.common.util.WeekCalculator;
import com.ktb.marong.domain.feed.Post;
import com.ktb.marong.domain.feed.PostLike;
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
import java.time.LocalDateTime;
import java.util.List;
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
     * 게시글 업로드 (그룹 ID 파라미터 추가)
     */
    @Transactional
    public Long savePost(Long userId, Long groupId, PostRequestDto requestDto, MultipartFile image) {
        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 사용자가 해당 그룹에 속해있는지 확인
        if (!userGroupRepository.existsByUserIdAndGroupId(userId, groupId)) {
            throw new CustomException(ErrorCode.GROUP_NOT_FOUND, "해당 그룹에 속하지 않은 사용자입니다.");
        }

        // 미션 조회
        Mission mission = missionRepository.findById(requestDto.getMissionId())
                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_NOT_FOUND));

        // 현재 주차 계산
        int currentWeek = WeekCalculator.getCurrentWeek();

        // 미션이 현재 사용자에게 할당된 것인지 확인 (그룹 ID 포함)
        UserMission userMission = userMissionRepository.findByUserIdAndGroupIdAndMissionIdAndWeek(
                        userId, groupId, requestDto.getMissionId(), currentWeek)
                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_NOT_ASSIGNED, "할당되지 않은 미션입니다."));

        // 미션 상태가 진행 중인지 확인
        if (!"ing".equals(userMission.getStatus())) {
            throw new CustomException(ErrorCode.MISSION_ALREADY_COMPLETED, "이미 완료된 미션입니다.");
        }

        // 해당 미션을 현재 주차에 이미 수행했는지 확인 - 그룹 ID 포함하여 체크
        int postCount = postRepository.countByUserIdAndMissionIdAndWeekAndGroupId(
                userId, requestDto.getMissionId(), currentWeek, groupId);
        log.info("미션 수행 확인: userId={}, missionId={}, week={}, groupId={}, postCount={}",
                userId, requestDto.getMissionId(), currentWeek, groupId, postCount);

        if (postCount > 0) {
            throw new CustomException(ErrorCode.MISSION_ALREADY_COMPLETED, "이번 주차에 이미 해당 미션을 완료했습니다.");
        }

        // 현재 사용자의 마니띠 정보 조회 (그룹 ID 포함)
        List<Manitto> manittoList = manittoRepository.findByManittoIdAndGroupIdAndWeek(userId, groupId, currentWeek);
        if (manittoList.isEmpty()) {
            throw new CustomException(ErrorCode.MANITTO_NOT_FOUND);
        }

        Manitto manitto = manittoList.get(0);
        String manitteeName = manitto.getManittee().getNickname(); // 마니띠(대상자)의 이름

        // 익명 이름 조회 - 현재 주차 및 그룹 정보 추가
        String anonymousName = anonymousNameRepository.findAnonymousNameByUserIdAndGroupIdAndWeek(
                        userId, groupId, currentWeek)
                .orElse("익명의 " + getRandomAnimal()); // 없으면 기본 이름 생성

        // 이미지 업로드 처리
        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            try {
                imageUrl = fileUploadService.uploadFile(image, "feeds"); // 인터페이스 메서드 호출
            } catch (IOException e) {
                log.error("이미지 업로드 실패: {}", e.getMessage());
                throw new CustomException(ErrorCode.FILE_UPLOAD_ERROR);
            }
        }

        // 게시글 생성 - 그룹 ID 및 주차 정보 포함
        Post post = Post.builder()
                .user(user)
                .groupId(groupId) // 파라미터로 받은 그룹 ID 사용
                .mission(mission)
                .week(currentWeek) // 주차 정보 추가
                .anonymousSnapshotName(anonymousName)
                .manitteeName(manitteeName)
                .content(requestDto.getContent())
                .imageUrl(imageUrl)
                .build();

        // 게시글 저장
        Post savedPost = postRepository.save(post);

        // 미션 완료 상태 업데이트 (UserMission 테이블)
        updateMissionStatus(userId, groupId, mission.getId(), currentWeek);

        return savedPost.getId();
    }

    /**
     * MVP 호환성을 위한 오버로드 메서드 (기본 그룹 ID 1 사용)
     */
    @Transactional
    public Long savePost(Long userId, PostRequestDto requestDto, MultipartFile image) {
        return savePost(userId, 1L, requestDto, image);
    }

    // 익명 이름 기본값 반환을 위한 랜덤 동물 이름 생성 메소드 추가
    private String getRandomAnimal() {
        String[] animals = {"판다", "고양이", "강아지", "호랑이", "코끼리", "원숭이", "토끼"};
        return animals[new Random().nextInt(animals.length)];
    }

    /**
     * 게시글 좋아요 등록/취소
     */
    @Transactional
    public PostLikeResponseDto toggleLike(Long userId, Long feedId, PostLikeRequestDto requestDto) {
        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 게시글 조회
        Post post = postRepository.findById(feedId)
                .orElseThrow(() -> new CustomException(ErrorCode.FEED_NOT_FOUND));

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
     * 게시글 목록 조회 (그룹 ID 파라미터 추가)
     */
    @Transactional(readOnly = true)
    public PostPageResponseDto getPosts(Long userId, Long groupId, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page - 1, pageSize);

        Page<Post> postPage;
        if (groupId != null) {
            // 특정 그룹의 게시글만 조회
            postPage = postRepository.findAllByGroupIdOrderByCreatedAtDesc(groupId, pageable);
        } else {
            // 모든 게시글 조회 (MVP 호환성)
            postPage = postRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        List<PostResponseDto> postDtos = postPage.getContent().stream()
                .map(post -> {
                    int likeCount = postLikeRepository.countByPostId(post.getId());
                    boolean isLiked = postLikeRepository.existsByUserAndPost(
                            userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND)),
                            post);
                    return PostResponseDto.fromEntity(post, likeCount, isLiked);
                })
                .collect(Collectors.toList());

        return PostPageResponseDto.builder()
                .page(page)
                .pageSize(pageSize)
                .totalFeeds((int) postPage.getTotalElements())
                .feeds(postDtos)
                .build();
    }

    /**
     * MVP 호환성을 위한 오버로드 메서드 (그룹 필터링 없음)
     */
    @Transactional(readOnly = true)
    public PostPageResponseDto getPosts(Long userId, int page, int pageSize) {
        return getPosts(userId, null, page, pageSize);
    }

    /**
     * 미션 완료 상태 업데이트 (그룹 ID 파라미터 추가)
     */
    private void updateMissionStatus(Long userId, Long groupId, Long missionId, Integer week) {
        // UserMission 테이블에서 미션 상태를 'completed'로 업데이트
        userMissionRepository.findByUserIdAndGroupIdAndMissionIdAndWeek(userId, groupId, missionId, week)
                .ifPresent(userMission -> {
                    userMission.complete();
                    userMissionRepository.save(userMission);
                    log.info("미션 완료 상태 업데이트: userId={}, groupId={}, missionId={}, week={}",
                            userId, groupId, missionId, week);
                });
    }

    /**
     * 주차 정보 마이그레이션 - 기존 게시글에 주차 정보 추가
     */
    @Transactional
    public void migratePostWeekData() {
        // 기존 데이터에 대해 게시글 생성일자를 기준으로 주차 계산
        List<Post> allPosts = postRepository.findAll();

        for (Post post : allPosts) {
            // 게시글 생성일자를 기준으로 주차 계산
            LocalDateTime createdAt = post.getCreatedAt();
            int week = WeekCalculator.getWeekOf(createdAt.toLocalDate());

            // 주차 정보가 없거나 0인 경우에만 업데이트
            if (post.getWeek() == null || post.getWeek() == 0) {
                // 엔티티를 직접 수정할 수 없으므로 새 엔티티 생성 후 저장
                Post updatedPost = Post.builder()
                        .user(post.getUser())
                        .groupId(post.getGroupId()) // 기존 groupId 유지
                        .mission(post.getMission())
                        .week(week)
                        .anonymousSnapshotName(post.getAnonymousSnapshotName())
                        .manitteeName(post.getManitteeName())
                        .content(post.getContent())
                        .imageUrl(post.getImageUrl())
                        .build();

                // ID 설정 (기존 ID 유지)
                updatedPost.setId(post.getId());

                // 저장
                postRepository.save(updatedPost);

                log.info("게시글 주차 정보 마이그레이션: postId={}, week={}, groupId={}",
                        post.getId(), week, post.getGroupId());
            }
        }
    }
}