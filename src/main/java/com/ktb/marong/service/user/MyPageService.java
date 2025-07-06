package com.ktb.marong.service.user;

import com.ktb.marong.domain.feed.Post;
import com.ktb.marong.domain.group.Group;
import com.ktb.marong.domain.group.UserGroup;
import com.ktb.marong.domain.user.MbtiUpdates;
import com.ktb.marong.domain.user.User;
import com.ktb.marong.dto.response.user.MyPagePostResponseDto;
import com.ktb.marong.exception.CustomException;
import com.ktb.marong.exception.ErrorCode;
import com.ktb.marong.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MyPageService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final MbtiUpdatesRepository mbtiUpdatesRepository;
    private final UserGroupRepository userGroupRepository;

    /**
     * 마이페이지용 사용자가 작성한 게시글 목록 조회 (그룹별로 묶어서)
     */
    @Transactional(readOnly = true)
    public List<MyPagePostResponseDto> getMyPosts(Long userId, int page, int pageSize) {
        log.info("마이페이지 그룹별 게시글 조회: userId={}, page={}, pageSize={}", userId, page, pageSize);

        // 1. 사용자 존재 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. 페이지네이션 설정
        Pageable pageable = PageRequest.of(page - 1, pageSize);

        // 3. 사용자가 작성한 게시글 조회
        Page<Post> postPage = postRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        // 4. 그룹별로 게시글 분류
        Map<Long, List<Post>> postsByGroup = postPage.getContent().stream()
                .collect(Collectors.groupingBy(Post::getGroupId));

        // 5. 그룹별 DTO 생성 (그룹별 최신 게시글 순으로 정렬)
        return postsByGroup.entrySet().stream()
                .map(entry -> {
                    Long groupId = entry.getKey();
                    List<Post> groupPosts = entry.getValue();

                    // 그룹 정보 조회
                    Group group = groupRepository.findById(groupId).orElse(null);

                    MyPagePostResponseDto.GroupInfo groupInfo = null;
                    if (group != null) {
                        groupInfo = MyPagePostResponseDto.GroupInfo.builder()
                                .groupId(group.getId())
                                .groupName(group.getName())
                                .groupImageUrl(group.getImageUrl())
                                .postCount(groupPosts.size())
                                .build();
                    }

                    // 해당 그룹의 게시글들을 PostInfo로 변환 (그룹 내에서는 최신순)
                    List<MyPagePostResponseDto.PostInfo> postInfos = groupPosts.stream()
                            .sorted((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()))
                            .map(post -> convertToPostInfo(post, userId))
                            .collect(Collectors.toList());

                    return MyPagePostResponseDto.builder()
                            .groupInfo(groupInfo)
                            .posts(postInfos)
                            .build();
                })
                // 그룹들을 최신 게시글 기준으로 정렬
                .sorted((g1, g2) -> {
                    LocalDateTime latestTime1 = g1.getPosts().get(0).getCreatedAt();
                    LocalDateTime latestTime2 = g2.getPosts().get(0).getCreatedAt();
                    return latestTime2.compareTo(latestTime1);
                })
                .collect(Collectors.toList());
    }

    /**
     * Post를 PostInfo로 변환 (모든 게시글 정보 포함)
     */
    private MyPagePostResponseDto.PostInfo convertToPostInfo(Post post, Long userId) {
        // 1. 기본 게시글 정보
        int likeCount = postLikeRepository.countByPostId(post.getId());

        // 사용자 조회 (좋아요 확인용)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        boolean isLiked = postLikeRepository.existsByUserAndPost(user, post);

        // 2. 해당 그룹에서의 사용자 프로필 정보 조회
        UserGroup userGroup = userGroupRepository.findByUserIdAndGroupId(userId, post.getGroupId())
                .orElse(null);

        String authorProfileImageUrl = null;
        if (userGroup != null) {
            authorProfileImageUrl = userGroup.getGroupUserProfileImageUrl();
        }

        // 3. MBTI 업데이트 정보 조회
        Optional<MbtiUpdates> mbtiUpdateOpt = mbtiUpdatesRepository.findByPostId(post.getId());
        MyPagePostResponseDto.MbtiUpdateInfo mbtiUpdateInfo = null;

        if (mbtiUpdateOpt.isPresent()) {
            MbtiUpdates mbtiUpdate = mbtiUpdateOpt.get();
            mbtiUpdateInfo = MyPagePostResponseDto.MbtiUpdateInfo.builder()
                    .changedMbtiType(mbtiUpdate.getChangedMbtiType())
                    .changeReason(mbtiUpdate.getChangeReason())
                    .previousScore(mbtiUpdate.getPreviousScore())
                    .currentScore(mbtiUpdate.getCurrentScore())
                    .updatedAt(mbtiUpdate.getCreatedAt())
                    .build();
        }

        // 4. PostInfo DTO 생성
        return MyPagePostResponseDto.PostInfo.builder()
                .feedId(post.getId())
                .anonymousAuthorName(post.getAnonymousSnapshotName())
                .authorProfileImageUrl(authorProfileImageUrl)
                .missionTitle(post.getMission().getTitle())
                .manitteeName(post.getManitteeName())
                .content(post.getContent())
                .likes(likeCount)
                .createdAt(post.getCreatedAt())
                .imageUrl(post.getImageUrl())
                .week(post.getWeek())
                .liked(isLiked)
                .mbtiUpdate(mbtiUpdateInfo)
                .build();
    }
}