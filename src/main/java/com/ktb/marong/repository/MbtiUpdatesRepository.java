package com.ktb.marong.repository;

import com.ktb.marong.domain.user.MbtiUpdates;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MbtiUpdatesRepository extends JpaRepository<MbtiUpdates, Long> {

    /**
     * 특정 게시글의 가장 최신 MBTI 업데이트 정보 조회
     */
    @Query("SELECT m FROM MbtiUpdates m WHERE m.post.id = :postId ORDER BY m.createdAt DESC LIMIT 1")
    Optional<MbtiUpdates> findByPostId(@Param("postId") Long postId);

    /**
     * 특정 게시글의 모든 MBTI 업데이트 정보 조회 (최신순)
     */
    @Query("SELECT m FROM MbtiUpdates m WHERE m.post.id = :postId ORDER BY m.createdAt DESC")
    List<MbtiUpdates> findAllByPostIdOrderByCreatedAtDesc(@Param("postId") Long postId);

    /**
     * 특정 사용자의 모든 MBTI 업데이트 정보 조회 (최신순)
     */
    @Query("SELECT m FROM MbtiUpdates m WHERE m.user.id = :userId ORDER BY m.createdAt DESC")
    List<MbtiUpdates> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    /**
     * 특정 사용자의 특정 그룹에서의 MBTI 업데이트 정보 조회
     */
    @Query("SELECT m FROM MbtiUpdates m JOIN m.post p WHERE m.user.id = :userId AND p.groupId = :groupId ORDER BY m.createdAt DESC")
    List<MbtiUpdates> findByUserIdAndGroupIdOrderByCreatedAtDesc(@Param("userId") Long userId, @Param("groupId") Long groupId);

    /**
     * 특정 게시글에 MBTI 업데이트 정보가 있는지 확인
     */
    boolean existsByPostId(Long postId);
}