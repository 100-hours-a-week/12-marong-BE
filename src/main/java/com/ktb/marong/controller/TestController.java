package com.ktb.marong.controller;

import com.ktb.marong.domain.user.User;
import com.ktb.marong.dto.response.common.ApiResponse;
import com.ktb.marong.exception.CustomException;
import com.ktb.marong.exception.ErrorCode;
import com.ktb.marong.repository.UserRepository;
import com.ktb.marong.service.auth.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 테스트 전용 컨트롤러
 * 로컬 환경에서 테스트를 위한 API 제공
 */
@Slf4j
@Profile("local")
@RestController
@RequiredArgsConstructor
@RequestMapping("/test")
public class TestController {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    /**
     * 테스트용 JWT 토큰 발급 API
     * 실제 운영 환경에서는 비활성화 필요
     */
    @GetMapping("/token/{userId}")
    public ResponseEntity<?> getTestToken(@PathVariable Long userId) {
        log.info("테스트 토큰 요청: userId={}", userId);

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // JWT 토큰 생성
        String accessToken = jwtService.createAccessToken(user);
        String refreshToken = jwtService.createRefreshToken(user);

        // 응답 데이터 구성
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("userId", user.getId());
        responseData.put("nickname", user.getNickname());
        responseData.put("jwt", accessToken);
        responseData.put("refreshToken", refreshToken);

        return ResponseEntity.ok(ApiResponse.success(
                responseData,
                "test_token_issued",
                null
        ));
    }
}
