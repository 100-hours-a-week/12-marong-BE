package com.ktb.marong.repository;

import com.ktb.marong.domain.survey.SurveyMBTI;
import com.ktb.marong.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * MBTI 설문 리포지토리
 */
@Repository
public interface SurveyMBTIRepository extends JpaRepository<SurveyMBTI, Long> {
    // 사용자에 해당하는 모든 MBTI 조회
    List<SurveyMBTI> findAllByUser(User user);

    // 사용자에 해당하는 모든 MBTI 삭제
    void deleteAllByUser(User user);

    // 추가: 사용자 ID로 직접 삭제하는 메소드 (더 효율적인 삭제 처리)
    @Modifying
    @Query("DELETE FROM SurveyMBTI s WHERE s.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    // 추가: 사용자의 가장 최신 MBTI 정보 조회
    SurveyMBTI findFirstByUserOrderByCreatedAtDesc(User user);

    // 추가: 사용자 ID로 가장 최신 MBTI 정보 조회
    SurveyMBTI findFirstByUser_IdOrderByCreatedAtDesc(Long userId);
}