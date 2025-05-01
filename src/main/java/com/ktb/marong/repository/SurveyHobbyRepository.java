package com.ktb.marong.repository;

import com.ktb.marong.domain.survey.SurveyHobby;
import com.ktb.marong.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SurveyHobbyRepository extends JpaRepository<SurveyHobby, Long> {
    List<SurveyHobby> findAllByUser(User user);

    void deleteAllByUser(User user);
}