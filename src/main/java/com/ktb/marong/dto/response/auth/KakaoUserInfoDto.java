package com.ktb.marong.dto.response.auth;

import com.ktb.marong.domain.user.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 카카오 사용자 정보 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KakaoUserInfoDto {
    private String providerId;
    private String email;
    private String nickname;
    private String profileImageUrl;
}