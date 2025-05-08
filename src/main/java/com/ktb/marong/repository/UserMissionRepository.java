package com.ktb.marong.repository;

import com.ktb.marong.domain.mission.UserMission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * 특정 날짜 이전에 할당된 진행 중인 미션을 찾는 쿼리
     * 완료되지 않고 지나간 미션들을 처리하기 위함
     */
    @Query("SELECT um FROM UserMission um WHERE um.user.id = :userId AND um.assignedDate < :date AND um.status = 'ing'")
    List<UserMission> findIncompleteMissionsBeforeDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    /**
     * 특정 상태와 주차에 해당하는 모든 미션 조회
     * 주로 이전 주차의 진행 중인 미션을 모두 찾아 미완료 처리하기 위해 사용
     */
    List<UserMission> findByStatusAndWeek(String status, Integer week);
}