package com.ktb.marong.repository;

import com.ktb.marong.domain.survey.SurveyLikedFood;
import com.ktb.marong.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SurveyLikedFoodRepository extends JpaRepository<SurveyLikedFood, Long> {
    List<SurveyLikedFood> findAllByUser(User user);

    void deleteAllByUser(User user);
}