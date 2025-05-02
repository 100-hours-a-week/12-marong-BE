package com.ktb.marong.dto.response.feed;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostPageResponseDto {
    private int page;
    private int pageSize;
    private int totalFeeds;
    private List<PostResponseDto> feeds;
}