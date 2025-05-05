package com.ktb.marong.repository;

import com.ktb.marong.domain.user.AnonymousName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnonymousNameRepository extends JpaRepository<AnonymousName, Long> {

    // 수정된 메소드: 특정 사용자의 특정 주차에 해당하는 익명 이름 조회
    @Query("SELECT a.anonymousName FROM AnonymousName a WHERE a.user.id = :userId AND a.groupId = 1 AND a.week = :week")
    Optional<String> findAnonymousNameByUserIdAndWeek(@Param("userId") Long userId, @Param("week") Integer week);

    // 기존 메소드는 남겨두되 List로 변경
    @Query("SELECT a.anonymousName FROM AnonymousName a WHERE a.user.id = :userId AND a.groupId = 1")
    List<String> findAnonymousNamesByUserId(@Param("userId") Long userId);
}