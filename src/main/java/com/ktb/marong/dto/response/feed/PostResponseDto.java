package com.ktb.marong.dto.response.feed;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ktb.marong.domain.feed.Post;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE)
public class PostResponseDto {
    private Long feedId;
    private String author;
    private String missionTitle;
    private String manitteeName;
    private String content;
    private int likes;
    private LocalDateTime createdAt;
    private String imageUrl;
    private Integer week; // 주차 정보 추가

    @JsonProperty("isLiked")
    private boolean liked; // 필드명 변경: isLiked -> liked

    public static PostResponseDto fromEntity(Post post, int likesCount, boolean isLiked) {
        return PostResponseDto.builder()
                .feedId(post.getId())
                .author(post.getAnonymousSnapshotName())
                .missionTitle(post.getMission().getTitle())
                .manitteeName(post.getManitteeName())
                .content(post.getContent())
                .likes(likesCount)
                .createdAt(post.getCreatedAt())
                .imageUrl(post.getImageUrl())
                .week(post.getWeek()) // 주차 정보 포함
                .liked(isLiked)
                .build();
    }

    /**
     * 작성자 이름까지 커스터마이징 가능한 DTO 생성 메소드
     */
    public static PostResponseDto fromEntityWithRealTimeManitteeNameAndAuthor(Post post, int likesCount, boolean isLiked,
                                                                              String realTimeManitteeName, String customAuthorName) {
        return PostResponseDto.builder()
                .feedId(post.getId())
                .author(customAuthorName) // 커스터마이징된 작성자 이름 사용
                .missionTitle(post.getMission().getTitle())
                .manitteeName(realTimeManitteeName) // 실시간으로 결정된 마니띠 이름 사용
                .content(post.getContent())
                .likes(likesCount)
                .createdAt(post.getCreatedAt())
                .imageUrl(post.getImageUrl())
                .week(post.getWeek())
                .liked(isLiked)
                .build();
    }
}