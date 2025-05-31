package com.ktb.marong.dto.response.manitto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    private String remainingTime; // 다음 마니또 매칭까지 남은 시간 (HH:MM:SS)
    private Long groupId;
    private String groupName;
    @JsonIgnore
    private boolean isNewUser; // 신규 사용자 여부 플래그

    // 명시적인 getter 메소드로 JSON 직렬화 제어
    @JsonProperty("isNewUser")
    public boolean getIsNewUser() {
        return isNewUser;
    }

    // 마니또 공개 기간일 때 (금요일 17시 ~ 월요일 12시)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private RevealedManittoDto revealedManitto;

    // 일반 활동 기간일 때 (월요일 12시 ~ 금요일 17시)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private PreviousCycleManittoDto previousCycleManitto;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private CurrentManittoDto currentManitto;

    @JsonInclude(JsonInclude.Include.NON_NULL)
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