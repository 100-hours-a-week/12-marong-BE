package com.ktb.marong.dto.response.recommendation;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

public class PlaceLikeResponseDto {

    // 토글 응답 DTO (좋아요 등록/취소 통합)
    @Getter
    @Builder
    public static class ToggleResponse {
        private Long userId;
        private Long placeRecommendationId;
        private String placeName;
        private String placeType;

        @JsonProperty("isLiked")
        private boolean liked;// 현재 좋아요 상태
        private long totalLikes; // 총 좋아요 수
        private LocalDateTime likedAt; // 좋아요 등록 시간 (취소시 null)
    }

    // 사용자 좋아요 리스트 DTO
    @Getter
    @Builder
    public static class UserLikes {
        private Long userId;
        private int totalLikedPlaces;
        private List<LikedPlace> likedPlaces;

        @Getter
        @Builder
        public static class LikedPlace {
            private Long placeRecommendationId;
            private String placeName;
            private String placeType;
            private String category;
            private String address;
            private LocalDateTime likedAt;
        }
    }
}