package com.ktb.marong.repository;

import com.ktb.marong.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * User 리포지토리
 * 사용자 CRUD 작업을 위한 리포지토리
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 이메일로 사용자 조회
     */
    Optional<User> findByEmail(String email);

    /**
     * 제공자 ID로 사용자 조회
     */
    Optional<User> findByProviderId(String providerId);

    /**
     * 이메일 존재 여부 확인
     */
    boolean existsByEmail(String email);

    /**
     * 제공자 ID 존재 여부 확인
     */
    boolean existsByProviderId(String providerId);
}