package com.ktb.marong.dto.request.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 토큰 리프레시 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TokenRefreshRequestDto {
    private String refreshToken;
}