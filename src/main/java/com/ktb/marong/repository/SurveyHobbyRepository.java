package com.ktb.marong.repository;

import com.ktb.marong.domain.survey.SurveyHobby;
import com.ktb.marong.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 취미 설문 리포지토리
 */
@Repository
public interface SurveyHobbyRepository extends JpaRepository<SurveyHobby, Long> {
    // 사용자에 해당하는 모든 취미 조회
    List<SurveyHobby> findAllByUser(User user);

    // 사용자에 해당하는 모든 취미 삭제
    void deleteAllByUser(User user);

    // 추가: 사용자 ID로 직접 삭제하는 메소드 (더 효율적인 삭제 처리)
    @Modifying
    @Query("DELETE FROM SurveyHobby s WHERE s.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}