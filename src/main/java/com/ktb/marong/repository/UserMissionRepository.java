package com.ktb.marong.repository;

import com.ktb.marong.domain.mission.UserMission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserMissionRepository extends JpaRepository<UserMission, Long> {

    /**
     * 특정 사용자, 그룹, 주차에 해당하는 미션 목록 조회
     */
    List<UserMission> findByUserIdAndGroupIdAndWeek(Long userId, Long groupId, Integer week);

    /**
     * 특정 사용자의 특정 날짜 미션 존재 여부 확인
     */
    boolean existsByUserIdAndAssignedDate(Long userId, LocalDate assignedDate);

    /**
     * 특정 사용자, 미션, 주차에 해당하는 미션 조회
     * 게시글 작성 시 미션 검증에 필요한 메소드
     */
    Optional<UserMission> findByUserIdAndMissionIdAndWeek(Long userId, Long missionId, Integer week);
}