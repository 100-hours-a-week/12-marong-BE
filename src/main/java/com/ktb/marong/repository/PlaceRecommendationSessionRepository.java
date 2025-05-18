package com.ktb.marong.repository;

import com.ktb.marong.domain.recommendation.PlaceRecommendationSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlaceRecommendationSessionRepository extends JpaRepository<PlaceRecommendationSession, Long> {
    List<PlaceRecommendationSession> findByManittoIdAndWeek(Long manittoId, Integer week);
}