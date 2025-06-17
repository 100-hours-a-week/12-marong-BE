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
    private String role; // "manitto", "manittee", "none" (매칭 없을 경우)
    private String remainingTime; // 다음 마니또 공개까지 남은 시간 (모든 경우 공통)

    // 마니또 정보 (role이 "manitto"일 때만 값 존재)
    private String manitteeName; // 마니띠 실명
    private String manitteeProfileImage; // 마니띠 프로필 이미지

    // 마니띠 정보 (role이 "manittee"일 때만 값 존재)
    private String manittoAnonymousName; // 마니또 익명 이름
}