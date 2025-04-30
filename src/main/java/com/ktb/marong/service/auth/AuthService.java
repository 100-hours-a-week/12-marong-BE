package com.ktb.marong.service.auth;

import com.ktb.marong.domain.user.User;
import com.ktb.marong.dto.request.auth.TokenRefreshRequestDto;
import com.ktb.marong.dto.response.auth.KakaoUserInfoDto;
import com.ktb.marong.dto.response.auth.LoginResponseDto;
import com.ktb.marong.dto.response.auth.TokenRefreshResponseDto;
import com.ktb.marong.dto.response.auth.UserResponseDto;
import com.ktb.marong.exception.CustomException;
import com.ktb.marong.exception.ErrorCode;
import com.ktb.marong.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 서비스
 * 로그인, 로그아웃, 토큰 갱신 등의 인증 관련 기능을 제공하는 서비스입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final KakaoOAuthService kakaoOAuthService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    /**
     * OAuth 리디렉션 URL 생성
     */
    public String getOAuthRedirectUrl(String provider) {
        if (!"kakao".equals(provider)) {
            throw new CustomException(ErrorCode.INVALID_PROVIDER);
        }

        return kakaoOAuthService.getRedirectUrl();
    }

    /**
     * 소셜 로그인 처리
     */
    @Transactional
    public LoginResponseDto socialLogin(String provider, String code) {
        log.info("소셜 로그인 요청: provider={}, code={}", provider, code);

        if (!"kakao".equals(provider)) {
            throw new CustomException(ErrorCode.INVALID_PROVIDER);
        }

        try {
            // 카카오 액세스 토큰 획득
            String kakaoAccessToken = kakaoOAuthService.getAccessToken(code);

            // 카카오 사용자 정보 획득
            KakaoUserInfoDto userInfo = kakaoOAuthService.getUserInfo(kakaoAccessToken);

            // 카카오 사용자 정보로 사용자 조회 또는 생성
            User user = kakaoOAuthService.findOrCreateUser(userInfo);

            // JWT 토큰 발급
            String jwt = jwtService.createAccessToken(user);
            String refreshToken = jwtService.createRefreshToken(user);

            // 로그인 응답 생성
            return LoginResponseDto.builder()
                    .jwt(jwt)
                    .refreshToken(refreshToken)
                    .isNewUser(!user.getHasCompletedSurvey())
                    .user(UserResponseDto.fromEntity(user))
                    .build();
        } catch (Exception e) {
            log.error("소셜 로그인 처리 중 오류 발생", e);
            throw new CustomException(ErrorCode.SOCIAL_AUTH_FAILED);
        }
    }

    /**
     * 토큰 리프레시
     */
    @Transactional
    public TokenRefreshResponseDto refreshToken(TokenRefreshRequestDto requestDto) {
        String refreshToken = requestDto.getRefreshToken();

        if (refreshToken == null) {
            throw new CustomException(ErrorCode.MISSING_TOKEN);
        }

        // Refresh Token 유효성 검증
        if (!jwtService.validateToken(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        // Refresh Token에서 사용자 ID 추출
        String userId = jwtService.getUserIdFromToken(refreshToken);

        // 저장된 Refresh Token과 비교
        if (!jwtService.validateRefreshToken(userId, refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 사용자 조회
        User user = userRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 새로운 Access Token 발급
        String newAccessToken = jwtService.createAccessToken(user);

        return new TokenRefreshResponseDto(newAccessToken);
    }

    /**
     * 로그아웃 처리
     */
    @Transactional
    public void logout(Long userId) {
        // Refresh Token 삭제
        jwtService.deleteRefreshToken(userId.toString());
        log.info("로그아웃 처리 완료: userId={}", userId);
    }

    /**
     * 로그인 사용자 정보 조회
     */
    @Transactional(readOnly = true)
    public UserResponseDto getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return UserResponseDto.fromEntity(user);
    }
}