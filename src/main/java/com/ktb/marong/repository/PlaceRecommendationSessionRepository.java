package com.ktb.marong.repository;

import com.ktb.marong.domain.recommendation.PlaceRecommendationSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlaceRecommendationSessionRepository extends JpaRepository<PlaceRecommendationSession, Long> {

    /**
     * 마니또 ID와 주차로 세션 조회 (기존 메소드)
     */
    List<PlaceRecommendationSession> findByManittoIdAndWeek(Long manittoId, Integer week);

    /**
     * 마니또-마니띠 쌍과 주차로 정확한 세션 조회 (새로 추가)
     * 이 메소드 사용하면 정확한 매칭 세션을 찾을 수 있음
     */
    List<PlaceRecommendationSession> findByManittoIdAndManitteeIdAndWeek(Long manittoId, Long manitteeId, Integer week);
}