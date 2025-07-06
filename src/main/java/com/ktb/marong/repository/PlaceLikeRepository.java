package com.ktb.marong.repository;

import com.ktb.marong.domain.recommendation.PlaceLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlaceLikeRepository extends JpaRepository<PlaceLike, Long> {

    // 특정 사용자가 특정 장소 추천에 좋아요를 눌렀는지 확인
    boolean existsByUserIdAndPlaceRecommendationId(Long userId, Long placeRecommendationId);

    // 특정 사용자가 특정 장소 추천에 누른 좋아요 조회
    Optional<PlaceLike> findByUserIdAndPlaceRecommendationId(Long userId, Long placeRecommendationId);

    // 특정 사용자가 누른 모든 좋아요 조회
    List<PlaceLike> findByUserId(Long userId);

    // 특정 장소 추천의 좋아요 개수 조회
    long countByPlaceRecommendationId(Long placeRecommendationId);

    // 사용자가 특정 세션에서 좋아요한 장소들 조회
    @Query("SELECT pl FROM PlaceLike pl " +
            "JOIN pl.placeRecommendation pr " +
            "WHERE pl.user.id = :userId AND pr.session.id = :sessionId")
    List<PlaceLike> findByUserIdAndSessionId(@Param("userId") Long userId, @Param("sessionId") Long sessionId);
}