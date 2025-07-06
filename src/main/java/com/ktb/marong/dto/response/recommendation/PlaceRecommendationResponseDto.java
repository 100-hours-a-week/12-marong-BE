package com.ktb.marong.dto.response.recommendation;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceRecommendationResponseDto {
    private List<PlaceDto> restaurants;
    private List<PlaceDto> cafes;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlaceDto {
        private Long placeId;
        private String name;
        private String category;
        private String hours;
        private String address;
        private Double latitude;
        private Double longitude;

        @JsonProperty("isLiked")
        private boolean liked;
        private long totalLikes;
    }
}