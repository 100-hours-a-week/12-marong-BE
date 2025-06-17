package com.ktb.marong.dto.response.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 토큰 리프레시 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TokenRefreshResponseDto {
    private String jwt;
}