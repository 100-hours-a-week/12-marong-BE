package com.ktb.marong.controller;

import com.ktb.marong.dto.request.feed.PostLikeRequestDto;
import com.ktb.marong.dto.request.feed.PostRequestDto;
import com.ktb.marong.dto.response.common.ApiResponse;
import com.ktb.marong.dto.response.feed.PostLikeResponseDto;
import com.ktb.marong.dto.response.feed.PostPageResponseDto;
import com.ktb.marong.security.CurrentUser;
import com.ktb.marong.service.feed.FeedService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/feeds")
public class FeedController {

    private final FeedService feedService;

    /**
     * 게시글 업로드 (그룹 ID 파라미터 추가)
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadFeed(
            @CurrentUser Long userId,
            @RequestParam(required = false) Long groupId,
            @RequestParam("missionId") Long missionId,
            @RequestParam("content") String content,
            @RequestParam(value = "image", required = false) MultipartFile image) {

        // groupId가 없으면 기본 그룹(1) 사용 (MVP 호환성)
        Long targetGroupId = (groupId != null) ? groupId : 1L;

        log.info("게시글 업로드 요청: userId={}, groupId={}, missionId={}", userId, targetGroupId, missionId);

        PostRequestDto requestDto = new PostRequestDto(missionId, content);
        Long feedId = feedService.savePost(userId, targetGroupId, requestDto, image);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        Map.of("feedId", feedId, "groupId", targetGroupId),
                        "feed_uploaded",
                        null
                ));
    }

    /**
     * 게시글 좋아요 등록/취소
     */
    @PostMapping("/{feedId}/likes")
    public ResponseEntity<?> toggleLike(
            @CurrentUser Long userId,
            @PathVariable Long feedId,
            @Valid @RequestBody PostLikeRequestDto requestDto) {

        log.info("좋아요 요청: userId={}, feedId={}, cancel={}", userId, feedId, requestDto.getCancel());

        PostLikeResponseDto response = feedService.toggleLike(userId, feedId, requestDto);

        String message = requestDto.getCancel() ? "unlike_success" : "like_success";

        return ResponseEntity.ok(ApiResponse.success(
                response,
                message,
                null
        ));
    }

    /**
     * 게시글 목록 조회 (그룹 ID 파라미터 추가)
     */
    @GetMapping
    public ResponseEntity<?> getFeeds(
            @CurrentUser Long userId,
            @RequestParam(required = false) Long groupId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {

        log.info("피드 조회 요청: userId={}, groupId={}, page={}, pageSize={}", userId, groupId, page, pageSize);

        PostPageResponseDto response = feedService.getPosts(userId, groupId, page, pageSize);

        return ResponseEntity.ok(ApiResponse.success(
                response,
                "feeds_retrieved",
                null
        ));
    }
}