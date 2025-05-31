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
    private Long groupId; // 현재 조회한 그룹 ID
    private String groupName; // 현재 조회한 그룹 이름
    private List<PostResponseDto> feeds;
}