package com.ktb.marong.controller;

import com.ktb.marong.dto.response.common.ApiResponse;
import com.ktb.marong.dto.response.recommendation.PlaceRecommendationResponseDto;
import com.ktb.marong.exception.CustomException;
import com.ktb.marong.security.CurrentUser;
import com.ktb.marong.service.recommendation.PlaceRecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/recommendations")
public class RecommendationController {

    private final PlaceRecommendationService placeRecommendationService;

    /**
     * 장소 추천 조회 (밥집 & 카페)
     * MVP에서는 모든 사용자가 그룹 ID 1에 속한다고 가정
     */
    @GetMapping("/places")
    public ResponseEntity<?> getPlaceRecommendations(@CurrentUser Long userId) {
        log.info("장소 추천 요청: userId={}", userId);

        try {
            PlaceRecommendationResponseDto response = placeRecommendationService.getPlaceRecommendations(userId);

            return ResponseEntity.ok(ApiResponse.success(
                    response,
                    "places_recommended",
                    null
            ));
        } catch (CustomException e) {
            log.error("장소 추천 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(e.getErrorCode().getStatus())
                    .body(ApiResponse.error(e.getErrorCode().name(), e.getMessage()));
        }
    }
}