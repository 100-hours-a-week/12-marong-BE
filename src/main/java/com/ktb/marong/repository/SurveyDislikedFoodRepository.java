package com.ktb.marong.repository;

import com.ktb.marong.domain.survey.SurveyDislikedFood;
import com.ktb.marong.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SurveyDislikedFoodRepository extends JpaRepository<SurveyDislikedFood, Long> {
    List<SurveyDislikedFood> findAllByUser(User user);

    void deleteAllByUser(User user);
}