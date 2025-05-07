package com.ktb.marong.dto.response.recommendation;

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
        private String name;
        private String category;
        private String hours;
        private String address;
    }
}