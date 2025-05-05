package com.ktb.marong.service.feed;

import com.ktb.marong.domain.feed.Post;
import com.ktb.marong.domain.feed.PostLike;
import com.ktb.marong.domain.mission.Mission;
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
    private final FileUploadService fileUploadService;

    /**
     * 게시글 업로드
     */
    @Transactional
    public Long savePost(Long userId, PostRequestDto requestDto, MultipartFile image) {
        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 미션 조회
        Mission mission = missionRepository.findById(requestDto.getMissionId())
                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_NOT_FOUND));

        // 해당 미션을 이미 수행했는지 확인
        if (postRepository.countByUserIdAndMissionId(userId, requestDto.getMissionId()) > 0) {
            throw new CustomException(ErrorCode.MISSION_ALREADY_COMPLETED);
        }

//        // 사용자의 익명 이름 조회 (MVP에서는 그룹 ID가 1로 고정) -> 익명 이름 조회 실패 시 에러코드 출력
//        String anonymousName = anonymousNameRepository.findAnonymousNameByUserId(userId)
//                .orElseThrow(() -> new CustomException(ErrorCode.ANONYMOUS_NAME_NOT_FOUND));

        Manitto manitto = manittoList.get(0);
        String manittoName = manitto.getReceiver().getNickname();  // 서버에서 manittoName 설정

        // 익명 이름 조회 - 현재 주차 정보 추가
        String anonymousName = anonymousNameRepository.findAnonymousNameByUserIdAndWeek(userId, currentWeek)
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

        // 게시글 생성
        Post post = Post.builder()
                .user(user)
                .mission(mission)
                .anonymousSnapshotName(anonymousName)
                .manittoName(requestDto.getManittoName())
                .content(requestDto.getContent())
                .imageUrl(imageUrl)
                .build();

        // 게시글 저장
        Post savedPost = postRepository.save(post);

        // 미션 완료 상태 업데이트 (UserMission 테이블)
        updateMissionStatus(userId, mission.getId());

        return savedPost.getId();
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
     * 게시글 목록 조회 (MVP에서는 그룹 필터링 없이 모든 게시글 조회)
     */
    @Transactional(readOnly = true)
    public PostPageResponseDto getPosts(int page, int pageSize) {
        Pageable pageable = PageRequest.of(page - 1, pageSize);
        Page<Post> postPage = postRepository.findAllByOrderByCreatedAtDesc(pageable);

        List<PostResponseDto> postDtos = postPage.getContent().stream()
                .map(post -> {
                    int likeCount = postLikeRepository.countByPostId(post.getId());
                    return PostResponseDto.fromEntity(post, likeCount);
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
     * 미션 완료 상태 업데이트
     */
    private void updateMissionStatus(Long userId, Long missionId) {
        // UserMission 테이블에서 미션 상태를 'completed'로 업데이트
        // MVP에서는 간소화하여 실제 구현 생략 가능
        log.info("미션 완료 상태 업데이트: userId={}, missionId={}", userId, missionId);
    }
}