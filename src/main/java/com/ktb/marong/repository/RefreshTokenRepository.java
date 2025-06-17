package com.ktb.marong.repository;

import com.ktb.marong.domain.auth.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * RefreshToken 리포지토리
 * 리프레시 토큰 CRUD 작업을 위한 리포지토리
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * 사용자 ID로 리프레시 토큰 조회
     */
    Optional<RefreshToken> findByUserId(Long userId);

    /**
     * 토큰으로 리프레시 토큰 조회
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * 사용자 ID로 리프레시 토큰 삭제
     */
    void deleteByUserId(Long userId);
}