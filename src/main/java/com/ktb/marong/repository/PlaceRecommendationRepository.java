package com.ktb.marong.repository;

import com.ktb.marong.domain.recommendation.PlaceRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlaceRecommendationRepository extends JpaRepository<PlaceRecommendation, Long> {
    List<PlaceRecommendation> findBySessionIdAndType(Long sessionId, String type);

    List<PlaceRecommendation> findBySessionId(Long sessionId);
}