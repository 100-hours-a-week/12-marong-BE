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
    private String role; // "manitto", "manittee" (매칭이 있는 경우만)
    private ManitteeDto manittee; // 마니또인 경우에만 존재
    private ManittoDto manitto; // 마니띠인 경우에만 존재

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ManitteeDto {
        private String name;
        private String profileImage;
        private String remainingTime;  // 다음 마니또 공개까지 남은 시간 (HH:MM:SS 형식)
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ManittoDto {
        private String anonymousName; // 마니또가 사용하는 익명 이름
        private String remainingTime; // 다음 마니또 공개까지 남은 시간 (HH:MM:SS 형식)
    }
}