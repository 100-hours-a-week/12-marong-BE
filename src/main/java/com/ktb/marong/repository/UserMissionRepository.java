package com.ktb.marong.repository;

import com.ktb.marong.domain.mission.UserMission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserMissionRepository extends JpaRepository<UserMission, Long> {

    /**
     * 특정 사용자, 그룹, 주차에 해당하는 미션 목록 조회
     */
    List<UserMission> findByUserIdAndGroupIdAndWeek(Long userId, Long groupId, Integer week);
}