package com.ktb.marong.controller;

import com.ktb.marong.dto.response.common.ApiResponse;
import com.ktb.marong.dto.response.recommendation.PlaceRecommendationResponseDto;
import com.ktb.marong.exception.CustomException;
import com.ktb.marong.repository.GroupRepository;
import com.ktb.marong.security.CurrentUser;
import com.ktb.marong.service.recommendation.PlaceRecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/recommendations")
public class RecommendationController {

    private final PlaceRecommendationService placeRecommendationService;
    private final GroupRepository groupRepository;

    /**
     * 장소 추천 조회 (밥집 & 카페) - 그룹별 분리
     */
    @GetMapping("/places")
    public ResponseEntity<?> getPlaceRecommendations(
            @CurrentUser Long userId,
            @RequestParam(value = "groupId", required = true) Long groupId) { // required = true 명시적 설정

        log.info("장소 추천 요청: userId={}, groupId={}", userId, groupId);

        // groupId 유효성 검증 추가
        if (groupId == null || groupId <= 0) {
            log.warn("잘못된 groupId: {}", groupId);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_GROUP_ID", "유효하지 않은 그룹 ID입니다."));
        }

        // 그룹 존재 여부 확인
        if (!groupRepository.existsById(groupId)) {
            log.warn("존재하지 않는 그룹: groupId={}", groupId);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("GROUP_NOT_FOUND", "존재하지 않는 그룹입니다."));
        }

        try {
            PlaceRecommendationResponseDto response = placeRecommendationService.getPlaceRecommendations(userId, groupId);

            return ResponseEntity.ok(ApiResponse.success(
                    response,
                    "places_recommended",
                    null
            ));
        } catch (CustomException e) {
            log.error("장소 추천 조회 실패: userId={}, groupId={}, error={}", userId, groupId, e.getMessage());
            return ResponseEntity.status(e.getErrorCode().getStatus())
                    .body(ApiResponse.error(e.getErrorCode().name(), e.getMessage()));
        } catch (Exception e) {
            log.error("장소 추천 조회 중 예상치 못한 오류: userId={}, groupId={}", userId, groupId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("INTERNAL_SERVER_ERROR", "서버 오류입니다."));
        }
    }

    /**
     * MVP 호환성을 위한 기본 그룹 장소 추천
     */
    @Deprecated
    @GetMapping("/places/default")
    public ResponseEntity<?> getDefaultPlaceRecommendations(@CurrentUser Long userId) {
        log.info("기본 그룹 장소 추천 요청: userId={}", userId);

        try {
            // 기본 그룹 ID 1 사용 (MVP 호환성)
            PlaceRecommendationResponseDto response = placeRecommendationService.getPlaceRecommendations(userId, 1L);

            return ResponseEntity.ok(ApiResponse.success(
                    response,
                    "places_recommended",
                    null
            ));
        } catch (CustomException e) {
            log.error("기본 그룹 장소 추천 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(e.getErrorCode().getStatus())
                    .body(ApiResponse.error(e.getErrorCode().name(), e.getMessage()));
        } catch (Exception e) {
            log.error("기본 그룹 장소 추천 조회 중 예상치 못한 오류: userId={}", userId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("INTERNAL_SERVER_ERROR", "서버 오류입니다."));
        }
    }
}