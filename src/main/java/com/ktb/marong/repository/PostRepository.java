package com.ktb.marong.repository;

import com.ktb.marong.domain.feed.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    // 기존 메서드 유지 (필요시 사용) -> MVP에서는 모든 게시글을 최신순으로 가져옴 (그룹 필터링 없음)
    @Query("SELECT p FROM Post p ORDER BY p.createdAt DESC")
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // 주차별 게시글 조회 메서드 추가
    @Query("SELECT p FROM Post p WHERE p.week = :week ORDER BY p.createdAt DESC")
    Page<Post> findAllByWeekOrderByCreatedAtDesc(@Param("week") Integer week, Pageable pageable);

    // 특정 사용자, 특정 미션에 대한 게시글 수 조회 (기존 메서드)
    @Query("SELECT COUNT(p) FROM Post p WHERE p.user.id = :userId AND p.mission.id = :missionId")
    int countByUserIdAndMissionId(@Param("userId") Long userId, @Param("missionId") Long missionId);

    // 특정 사용자, 특정 미션, 특정 주차에 대한 게시글 수 조회 (수정된 메서드)
    @Query("SELECT COUNT(p) FROM Post p WHERE p.user.id = :userId AND p.mission.id = :missionId AND p.week = :week")
    int countByUserIdAndMissionIdAndWeek(
            @Param("userId") Long userId,
            @Param("missionId") Long missionId,
            @Param("week") Integer week);

    // 특정 날짜 범위의 게시글을 주차별로 업데이트하기 위한 메서드
    @Query("SELECT p FROM Post p WHERE p.createdAt BETWEEN :startDate AND :endDate")
    List<Post> findByCreatedAtBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}