package com.ktb.marong.service.auth;

import com.ktb.marong.domain.auth.RefreshToken;
import com.ktb.marong.domain.user.User;
import com.ktb.marong.repository.RefreshTokenRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * JWT 토큰 서비스
 * JWT 토큰 생성, 검증, 처리를 담당하는 서비스
 */
@Slf4j
@Service
public class JwtService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecretKey key;
    private final long accessTokenValidity;
    private final long refreshTokenValidity;

    public JwtService(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${jwt.secret}") String secretKey,
            @Value("${jwt.access-token-validity}") long accessTokenValidity,
            @Value("${jwt.refresh-token-validity}") long refreshTokenValidity) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidity = accessTokenValidity;
        this.refreshTokenValidity = refreshTokenValidity;
    }

    /**
     * Access Token을 생성
     */
    public String createAccessToken(User user) {
        return createToken(user.getId().toString(), user.getEmail(), accessTokenValidity);
    }

    /**
     * Refresh Token을 생성하고 저장
     */
    @Transactional
    public String createRefreshToken(User user) {
        String token = createToken(user.getId().toString(), user.getEmail(), refreshTokenValidity);

        // 토큰 만료일 계산
        LocalDateTime expiresAt = LocalDateTime.now().plusNanos(refreshTokenValidity * 1000000);

        // 기존 토큰이 있는지 조회하여 업데이트 또는 생성
        RefreshToken refreshToken = refreshTokenRepository.findByUserId(user.getId())
                .map(rt -> {
                    rt.updateToken(token, expiresAt);
                    return rt;
                })
                .orElse(RefreshToken.builder()
                        .userId(user.getId())
                        .token(token)
                        .expiresAt(expiresAt)
                        .build());

        refreshTokenRepository.save(refreshToken);

        return token;
    }

    /**
     * JWT 토큰을 생성
     */
    private String createToken(String subject, String email, long expireTime) {
        Claims claims = Jwts.claims().setSubject(subject);
        claims.put("email", email);

        Date now = new Date();
        Date expiration = new Date(now.getTime() + expireTime);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * HTTP 요청에서 JWT 토큰을 추출
     */
    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * JWT 토큰의 유효성을 검증
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.info("만료된 JWT 토큰입니다.");
        } catch (Exception e) {
            log.info("유효하지 않은 JWT 토큰입니다.");
        }
        return false;
    }

    /**
     * JWT 토큰에서 사용자 ID를 추출
     */
    public String getUserIdFromToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
        } catch (ExpiredJwtException e) {
            // 만료된 토큰에서도 userId는 추출 가능
            return e.getClaims().getSubject();
        }
    }

    /**
     * 저장된 Refresh Token과 일치하는지 확인
     */
    @Transactional(readOnly = true)
    public boolean validateRefreshToken(String userId, String refreshToken) {
        return refreshTokenRepository.findByUserId(Long.parseLong(userId))
                .map(token -> !token.isExpired() && token.getToken().equals(refreshToken))
                .orElse(false);
    }

    /**
     * 저장된 Refresh Token을 삭제 (로그아웃 시 사용)
     */
    @Transactional
    public void deleteRefreshToken(String userId) {
        refreshTokenRepository.deleteByUserId(Long.parseLong(userId));
    }
}