package com.ktb.marong.repository;

import com.ktb.marong.domain.survey.SurveyMBTI;
import com.ktb.marong.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SurveyMBTIRepository extends JpaRepository<SurveyMBTI, Long> {
    Optional<SurveyMBTI> findByUser(User user);

    void deleteByUser(User user);
}
