package com.ktb.marong.repository;

import com.ktb.marong.domain.group.Group;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

    /**
     * 그룹 이름으로 그룹 조회
     */
    Optional<Group> findByName(String name);

    /**
     * 초대 코드로 그룹 조회
     */
    Optional<Group> findByInviteCode(String inviteCode);

    /**
     * 그룹 이름 중복 확인
     */
    boolean existsByName(String name);

    /**
     * 정규화된 그룹 이름으로 중복 확인
     */
    boolean existsByNormalizedName(String normalizedName);

    /**
     * 초대 코드 중복 확인
     */
    boolean existsByInviteCode(String inviteCode);

    /**
     * 모든 그룹을 생성일시 기준 최신순으로 조회 (페이지네이션 지원)
     */
    @Query("SELECT g FROM Group g ORDER BY g.id DESC")
    Page<Group> findAllOrderByIdDesc(Pageable pageable);

    // 방어로직 메소드들 - null 상태인 normalized_name 처리

    /**
     * normalized_name이 null인 그룹들 중에서 실시간 정규화해서 중복체크
     * 방어로직용 메소드
     */
    @Query("SELECT COUNT(g) > 0 FROM Group g " +
            "WHERE g.normalizedName IS NULL " +
            "AND LOWER(REPLACE(REPLACE(REPLACE(TRIM(g.name), ' ', ''), CHAR(9), ''), CHAR(10), '')) = :normalizedName")
    boolean existsByNullNormalizedNameWithRuntimeNormalization(@Param("normalizedName") String normalizedName);

    /**
     * 그룹명 중복체크 - 방어로직 포함
     */
    @Query("SELECT COUNT(g) > 0 FROM Group g " +
            "WHERE (g.normalizedName = :normalizedName " +
            "OR (g.normalizedName IS NULL AND LOWER(REPLACE(REPLACE(REPLACE(TRIM(g.name), ' ', ''), CHAR(9), ''), CHAR(10), '')) = :normalizedName))")
    boolean existsByNormalizedNameWithFallback(@Param("normalizedName") String normalizedName);
}