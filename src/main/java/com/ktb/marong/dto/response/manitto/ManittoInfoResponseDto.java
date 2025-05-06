package com.ktb.marong.dto.response.manitto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManittoInfoResponseDto {
    private ManittoDto manitto;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ManittoDto {
        private String name;
        private String profileImage;
        private String remainingTime;  // 다음 마니또 공개까지 남은 시간 (HH:MM:SS 형식)
    }
}