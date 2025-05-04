package com.ktb.marong.repository;

import com.ktb.marong.domain.feed.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    // MVP에서는 모든 게시글을 최신순으로 가져옴 (그룹 필터링 없음)
    @Query("SELECT p FROM Post p ORDER BY p.createdAt DESC")
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT COUNT(p) FROM Post p WHERE p.user.id = :userId AND p.mission.id = :missionId")
    int countByUserIdAndMissionId(@Param("userId") Long userId, @Param("missionId") Long missionId);
}