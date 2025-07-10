package com.ktb.marong.controller;

import com.ktb.marong.dto.response.common.ApiResponse;
import com.ktb.marong.dto.response.user.MyPagePostResponseDto;
import com.ktb.marong.security.CurrentUser;
import com.ktb.marong.service.user.MyPageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/my-page")
public class MyPageController {

    private final MyPageService myPageService;

    /**
     * 마이페이지 - 내가 작성한 피드 목록 조회 (그룹별로 묶어서)
     */
    @GetMapping("/feeds")
    public ResponseEntity<?> getMyFeeds(
            @CurrentUser Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {

        log.info("마이페이지 그룹별 피드 조회 요청: userId={}, page={}, pageSize={}", userId, page, pageSize);

        try {
            MyPagePostResponseDto groupedFeeds = myPageService.getMyPosts(userId, page, pageSize);

            return ResponseEntity.ok(ApiResponse.success(
                    groupedFeeds,
                    "my_grouped_feeds_retrieved",
                    null
            ));

        } catch (Exception e) {
            log.error("마이페이지 그룹별 피드 조회 중 오류: userId={}, error={}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("INTERNAL_SERVER_ERROR", "서버 오류입니다."));
        }
    }
}