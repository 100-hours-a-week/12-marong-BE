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

    // 기본 메소드: 그룹별 익명 이름 조회
    @Query("SELECT a.anonymousName FROM AnonymousName a WHERE a.user.id = :userId AND a.groupId = :groupId AND a.week = :week")
    Optional<String> findAnonymousNameByUserIdAndGroupIdAndWeek(
            @Param("userId") Long userId,
            @Param("groupId") Long groupId,
            @Param("week") Integer week);

    // 기본 메소드: 그룹별 모든 익명 이름 조회
    @Query("SELECT a.anonymousName FROM AnonymousName a WHERE a.user.id = :userId AND a.groupId = :groupId")
    List<String> findAnonymousNamesByUserIdAndGroupId(@Param("userId") Long userId, @Param("groupId") Long groupId);

    // MVP 호환성을 위한 메소드들 (Deprecated 처리)

    /**
     * @deprecated MVP 호환성을 위해 유지. 새로운 코드에서는 findAnonymousNameByUserIdAndGroupIdAndWeek 사용
     */
    @Deprecated
    @Query("SELECT a.anonymousName FROM AnonymousName a WHERE a.user.id = :userId AND a.groupId = 1 AND a.week = :week")
    Optional<String> findAnonymousNameByUserIdAndWeek(@Param("userId") Long userId, @Param("week") Integer week);

    /**
     * @deprecated MVP 호환성을 위해 유지. 새로운 코드에서는 findAnonymousNamesByUserIdAndGroupId 사용
     */
    @Deprecated
    @Query("SELECT a.anonymousName FROM AnonymousName a WHERE a.user.id = :userId AND a.groupId = 1")
    List<String> findAnonymousNamesByUserId(@Param("userId") Long userId);
}