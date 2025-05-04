package com.ktb.marong.dto.response.feed;

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
public class PostResponseDto {
    private Long feedId;
    private String author;
    private String missionTitle;
    private String manittoName;
    private String content;
    private int likes;
    private LocalDateTime createdAt;
    private String imageUrl;

    public static PostResponseDto fromEntity(Post post, int likesCount) {
        return PostResponseDto.builder()
                .feedId(post.getId())
                .author(post.getAnonymousSnapshotName())
                .missionTitle(post.getMission().getTitle())
                .manittoName(post.getManittoName())
                .content(post.getContent())
                .likes(likesCount)
                .createdAt(post.getCreatedAt())
                .imageUrl(post.getImageUrl())
                .build();
    }
}