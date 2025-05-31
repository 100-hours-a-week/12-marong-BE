package com.ktb.marong.dto.response.manitto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManittoDetailResponseDto {

    private String period; // "MANITTO_REVEAL" 또는 "MANITTO_ACTIVE"
    private String remainingTime; // 다음 전환까지 남은 시간 (HH:MM:SS)
    private Long groupId;
    private String groupName;

    // 마니또 공개 기간일 때 (금요일 17시 ~ 월요일 12시)
    private RevealedManittoDto revealedManitto;

    // 일반 활동 기간일 때 (월요일 12시 ~ 금요일 17시)
    private PreviousCycleManittoDto previousCycleManitto;
    private CurrentManittoDto currentManitto;
    private CurrentManitteeDto currentManittee;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevealedManittoDto {
        private String name; // 카카오 실명
        private String groupNickname; // 그룹 내 닉네임
        private String groupProfileImage; // 그룹 내 프로필 이미지
        private String anonymousName; // 이번 주기에서 사용했던 익명 이름
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PreviousCycleManittoDto {
        private String name; // 카카오 실명
        private String groupNickname; // 그룹 내 닉네임
        private String groupProfileImage; // 그룹 내 프로필 이미지
        private String anonymousName; // 이전 주기에서 사용했던 익명 이름
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CurrentManittoDto {
        private String anonymousName; // 나를 담당하는 마니또의 익명 이름
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CurrentManitteeDto {
        private String name; // 카카오 실명
        private String groupNickname; // 그룹 내 닉네임 (없으면 카카오 이름)
        private String groupProfileImage; // 그룹 내 프로필 이미지
    }
}