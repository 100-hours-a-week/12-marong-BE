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
}