package com.ktb.marong.controller;

import com.ktb.marong.dto.request.recommendation.PlaceLikeRequestDto;
import com.ktb.marong.dto.response.common.ApiResponse;
import com.ktb.marong.dto.response.recommendation.PlaceLikeResponseDto;
import com.ktb.marong.dto.response.recommendation.PlaceRecommendationResponseDto;
import com.ktb.marong.exception.CustomException;
import com.ktb.marong.repository.GroupRepository;
import com.ktb.marong.security.CurrentUser;
import com.ktb.marong.service.recommendation.PlaceRecommendationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/recommendations")
public class PlaceRecommendationController {

    private final PlaceRecommendationService placeRecommendationService;
    private final GroupRepository groupRepository;

    /**
     * 장소 추천 조회 (밥집 & 카페) - 그룹별 분리
     */
    @GetMapping("/places")
    public ResponseEntity<?> getPlaceRecommendations(
            @CurrentUser Long userId,
            @RequestParam(value = "groupId", required = true) Long groupId) {

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

    /**
     * 장소 추천 좋아요 등록/취소
     */
    @PostMapping("/places/{placeId}/likes")
    public ResponseEntity<?> togglePlaceLike(
            @CurrentUser Long userId,
            @PathVariable Long placeId,
            @Valid @RequestBody PlaceLikeRequestDto requestDto) {

        log.info("장소 추천 좋아요 요청: userId={}, placeId={}, cancel={}",
                userId, placeId, requestDto.getCancel());

        try {
            PlaceLikeResponseDto.ToggleResponse response = placeRecommendationService.togglePlaceLike(userId, placeId, requestDto);

            String message = requestDto.getCancel() ? "unlike_success" : "like_success";

            return ResponseEntity.ok(ApiResponse.success(
                    response,
                    message,
                    null
            ));
        } catch (CustomException e) {
            log.error("장소 추천 좋아요 실패: userId={}, placeId={}, cancel={}, error={}",
                    userId, placeId, requestDto.getCancel(), e.getMessage());
            return ResponseEntity.status(e.getErrorCode().getStatus())
                    .body(ApiResponse.error(e.getErrorCode().name(), e.getMessage()));
        } catch (Exception e) {
            log.error("장소 추천 좋아요 중 예상치 못한 오류: userId={}, placeId={}, cancel={}",
                    userId, placeId, requestDto.getCancel(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("INTERNAL_SERVER_ERROR", "서버 오류입니다."));
        }
    }

    /**
     * 장소 추천 좋아요 상태 확인
     */
    @GetMapping("/places/{placeId}/likes/status")
    public ResponseEntity<?> checkLikeStatus(
            @CurrentUser Long userId,
            @PathVariable Long placeId) {

        log.info("장소 추천 좋아요 상태 확인 요청: userId={}, placeId={}", userId, placeId);

        try {
            boolean isLiked = placeRecommendationService.isPlaceLikedByUser(userId, placeId);

            return ResponseEntity.ok(ApiResponse.success(
                    isLiked,
                    "like_status_retrieved",
                    null
            ));
        } catch (Exception e) {
            log.error("장소 추천 좋아요 상태 확인 중 예상치 못한 오류: userId={}, placeId={}",
                    userId, placeId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("INTERNAL_SERVER_ERROR", "서버 오류입니다."));
        }
    }

    /**
     * 사용자 좋아요 장소 목록 조회
     */
    @GetMapping("/places/likes/my")
    public ResponseEntity<?> getUserLikedPlaces(@CurrentUser Long userId) {

        log.info("사용자 좋아요 장소 목록 조회 요청: userId={}", userId);

        try {
            PlaceLikeResponseDto.UserLikes response = placeRecommendationService.getUserLikedPlaces(userId);

            return ResponseEntity.ok(ApiResponse.success(
                    response,
                    "user_liked_places_retrieved",
                    null
            ));
        } catch (CustomException e) {
            log.error("사용자 좋아요 장소 목록 조회 실패: userId={}, error={}", userId, e.getMessage());
            return ResponseEntity.status(e.getErrorCode().getStatus())
                    .body(ApiResponse.error(e.getErrorCode().name(), e.getMessage()));
        } catch (Exception e) {
            log.error("사용자 좋아요 장소 목록 조회 중 예상치 못한 오류: userId={}", userId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("INTERNAL_SERVER_ERROR", "서버 오류입니다."));
        }
    }
}