package com.ktb.marong.service.auth;

import com.ktb.marong.domain.user.User;
import com.ktb.marong.dto.response.auth.KakaoUserInfoDto;
import com.ktb.marong.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 카카오 OAuth 서비스
 * 카카오 소셜 로그인 처리를 담당하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoOAuthService {

    private final RestTemplate restTemplate;
    private final UserRepository userRepository;

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.kakao.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.registration.kakao.redirect-uri}")
    private String redirectUri;

    @Value("${spring.security.oauth2.client.provider.kakao.authorization-uri}")
    private String authorizationUri;

    @Value("${spring.security.oauth2.client.provider.kakao.token-uri}")
    private String tokenUri;

    @Value("${spring.security.oauth2.client.provider.kakao.user-info-uri}")
    private String userInfoUri;

    /**
     * 카카오 로그인 리디렉션 URL 생성
     */
    public String getRedirectUrl() {
        return authorizationUri + "?client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&response_type=code";
    }

    /**
     * 카카오 인증 코드를 통해 액세스 토큰을 얻음
     */
    public String getAccessToken(String code) {
        log.info("카카오 액세스 토큰 요청: code={}", code);

        // HTTP 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        // HTTP 요청 파라미터 설정
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("code", code);

        // HTTP 요청 설정
        HttpEntity<MultiValueMap<String, String>> kakaoTokenRequest = new HttpEntity<>(params, headers);

        try {
            // POST 요청 실행
            ResponseEntity<Map> response = restTemplate.exchange(
                    tokenUri,
                    HttpMethod.POST,
                    kakaoTokenRequest,
                    Map.class
            );

            log.info("카카오 액세스 토큰 응답: {}", response);
            log.info("카카오 액세스 토큰 응답: {}", response.getBody());

            // 응답에서 액세스 토큰 추출
            return (String) response.getBody().get("access_token");
        } catch (Exception e) {
            log.error("카카오 액세스 토큰 요청 실패", e);
            throw new RuntimeException("카카오 액세스 토큰 요청 실패: " + e.getMessage());
        }
    }

    /**
     * 카카오 액세스 토큰을 통해 사용자 정보를 얻음
     */
    public KakaoUserInfoDto getUserInfo(String accessToken) {
        log.info("카카오 사용자 정보 요청: accessToken={}", accessToken);

        // HTTP 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        // HTTP 요청 설정
        HttpEntity<MultiValueMap<String, String>> kakaoUserInfoRequest = new HttpEntity<>(headers);

        try {
            // GET 요청 실행
            ResponseEntity<Map> response = restTemplate.exchange(
                    userInfoUri,
                    HttpMethod.GET,
                    kakaoUserInfoRequest,
                    Map.class
            );

            log.info("카카오 사용자 정보 응답: {}", response.getBody());

            // 응답에서 사용자 정보 추출
            Map<String, Object> attributes = response.getBody();
            Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
            Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

            String providerId = String.valueOf(attributes.get("id"));
            String email = (String) kakaoAccount.get("email");
            String nickname = (String) profile.get("nickname");
            String profileImageUrl = (String) profile.get("profile_image_url");

            // 사용자 정보 DTO 생성
            return new KakaoUserInfoDto(providerId, email, nickname, profileImageUrl);
        } catch (Exception e) {
            log.error("카카오 사용자 정보 요청 실패", e);
            throw new RuntimeException("카카오 사용자 정보 요청 실패: " + e.getMessage());
        }
    }

    /**
     * 카카오 사용자 정보를 통해 신규 사용자를 등록하거나 기존 사용자를 찾음
     */
    @Transactional
    public User findOrCreateUser(KakaoUserInfoDto userInfoDto) {
        return userRepository.findByProviderId(userInfoDto.getProviderId())
                .orElseGet(() -> createUser(userInfoDto));
    }

    /**
     * 신규 사용자를 생성
     */
    private User createUser(KakaoUserInfoDto userInfoDto) {
        log.info("신규 사용자 등록: {}", userInfoDto.getEmail());

        User user = User.builder()
                .email(userInfoDto.getEmail())
                .providerId(userInfoDto.getProviderId())
                .nickname(userInfoDto.getNickname())
                .providerName("kakao")
                .profileImageUrl(userInfoDto.getProfileImageUrl())
                .build();

        return userRepository.save(user);
    }
}