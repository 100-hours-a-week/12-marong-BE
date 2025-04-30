package com.ktb.marong.controller;

import com.ktb.marong.dto.request.auth.TokenRefreshRequestDto;
import com.ktb.marong.dto.response.auth.LoginResponseDto;
import com.ktb.marong.dto.response.auth.TokenRefreshResponseDto;
import com.ktb.marong.dto.response.auth.UserResponseDto;
import com.ktb.marong.dto.response.common.ApiResponse;
import com.ktb.marong.security.CurrentUser;
import com.ktb.marong.service.auth.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 인증 컨트롤러
 * 로그인, 로그아웃, 토큰 갱신 등의 인증 관련 API를 제공
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    /**
     * 소셜 로그인 리디렉션 URL 요청
     */
    @GetMapping("/oauth/redirect-url")
    public ResponseEntity<?> getOAuthRedirectUrl(@RequestParam String provider) {
        log.info("OAuth 리디렉션 URL 요청: provider={}", provider);
        String redirectUrl = authService.getOAuthRedirectUrl(provider);
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("redirectUrl", redirectUrl),
                "redirect_url_generated",
                null
        ));
    }

    /**
     * 소셜 로그인 콜백 처리 (토큰 발급)
     */
    @GetMapping("/oauth/callback")
    public ResponseEntity<?> oauthCallback(@RequestParam String provider, @RequestParam String code) {
        log.info("OAuth 콜백 요청: provider={}, code={}", provider, code);
        LoginResponseDto loginResponse = authService.socialLogin(provider, code);
        return ResponseEntity.ok(ApiResponse.success(loginResponse, "login_success", null));
    }

    /**
     * 로그인 사용자 정보 조회
     */
    @GetMapping("/user")
    public ResponseEntity<?> getCurrentUser(@CurrentUser Long userId) {
        log.info("사용자 정보 조회: userId={}", userId);
        UserResponseDto userResponse = authService.getCurrentUser(userId);
        return ResponseEntity.ok(ApiResponse.success(userResponse, "user_info_retrieved", null));
    }

    /**
     * 로그아웃
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@CurrentUser Long userId) {
        log.info("로그아웃 요청: userId={}", userId);
        authService.logout(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "logout_success", null));
    }

    /**
     * 토큰 재발급 (Refresh Token)
     */
    @PostMapping("/token/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody TokenRefreshRequestDto requestDto) {
        log.info("토큰 재발급 요청");
        TokenRefreshResponseDto refreshResponse = authService.refreshToken(requestDto);
        return ResponseEntity.ok(ApiResponse.success(refreshResponse, "token_refreshed", null));
    }
}