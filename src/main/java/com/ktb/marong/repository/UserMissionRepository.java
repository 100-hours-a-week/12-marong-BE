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
     * 특정 사용자의 특정 날짜 미션 존재 여부 확인 (그룹 ID 추가)
     */
    boolean existsByUserIdAndGroupIdAndAssignedDate(Long userId, Long groupId, LocalDate assignedDate);

    /**
     * 특정 사용자, 미션, 주차에 해당하는 미션 조회
     * 게시글 작성 시 미션 검증 및 완료 여부 확인용
     */
    Optional<UserMission> findByUserIdAndMissionIdAndWeek(Long userId, Long missionId, Integer week);

    /**
     * 특정 사용자, 그룹, 미션, 주차에 해당하는 미션 조회
     */
    Optional<UserMission> findByUserIdAndGroupIdAndMissionIdAndWeek(Long userId, Long groupId, Long missionId, Integer week);

    /**
     * 특정 주차의 특정 날짜 이전에 할당된 진행 중인 미션을 찾는 쿼리 (그룹 ID 추가)
     */
    @Query("SELECT um FROM UserMission um WHERE um.user.id = :userId AND um.groupId = :groupId AND um.assignedDate < :date AND um.status = 'ing' AND um.week = :week")
    List<UserMission> findIncompleteMissionsBeforeDate(
            @Param("userId") Long userId,
            @Param("groupId") Long groupId,
            @Param("date") LocalDate date,
            @Param("week") Integer week);

    /**
     * 특정 주차의 오늘 할당된 진행 중인 미션 조회 (그룹 ID 추가)
     */
    @Query("SELECT um FROM UserMission um WHERE um.user.id = :userId AND um.groupId = :groupId AND um.assignedDate = :date AND um.status = 'ing' AND um.week = :week")
    List<UserMission> findTodaysMissions(
            @Param("userId") Long userId,
            @Param("groupId") Long groupId,
            @Param("date") LocalDate date,
            @Param("week") Integer week);

    /**
     * 특정 날짜와 주차에 할당된 모든 미션 조회 (상태 무관, 그룹 ID 추가)
     */
    @Query("SELECT um FROM UserMission um WHERE um.user.id = :userId AND um.groupId = :groupId AND um.assignedDate = :date AND um.week = :week")
    List<UserMission> findAllMissionsAssignedOnDate(
            @Param("userId") Long userId,
            @Param("groupId") Long groupId,
            @Param("date") LocalDate date,
            @Param("week") Integer week);

    /**
     * 특정 상태와 주차에 해당하는 모든 미션 조회
     * 주로 이전 주차의 진행 중인 미션을 모두 찾아 미완료 처리하기 위해 사용
     */
    List<UserMission> findByStatusAndWeek(String status, Integer week);

    /**
     * 특정 주차의 모든 미션 조회
     * 주차 변경 시 남아있는 미션을 초기화하기 위해 사용
     */
    List<UserMission> findByWeek(Integer week);

    /**
     * 특정 사용자, 그룹, 날짜에 할당된 진행 중인 미션 조회 (오늘의 활성 미션만)
     */
    @Query("SELECT um FROM UserMission um WHERE um.user.id = :userId AND um.groupId = :groupId AND um.assignedDate = :date AND um.status = 'ing' AND um.week = :week")
    List<UserMission> findTodaysActiveMissions(
            @Param("userId") Long userId,
            @Param("groupId") Long groupId,
            @Param("date") LocalDate date,
            @Param("week") Integer week);

    /**
     * 특정 그룹의 특정 주차에서 진행 중인 모든 미션 조회
     */
    @Query("SELECT um FROM UserMission um WHERE um.groupId = :groupId AND um.week = :week AND um.status = 'ing'")
    List<UserMission> findInProgressMissionsByGroupAndWeek(
            @Param("groupId") Long groupId,
            @Param("week") Integer week);

    /**
     * 특정 그룹의 특정 주차에서 완료된 모든 미션 조회
     */
    @Query("SELECT um FROM UserMission um WHERE um.groupId = :groupId AND um.week = :week AND um.status = 'completed'")
    List<UserMission> findCompletedMissionsByGroupAndWeek(
            @Param("groupId") Long groupId,
            @Param("week") Integer week);

    /**
     * 특정 그룹의 특정 주차에서 미완료된 모든 미션 조회
     */
    @Query("SELECT um FROM UserMission um WHERE um.groupId = :groupId AND um.week = :week AND um.status = 'incomplete'")
    List<UserMission> findIncompleteMissionsByGroupAndWeek(
            @Param("groupId") Long groupId,
            @Param("week") Integer week);

    /**
     * 특정 사용자의 특정 그룹에서 특정 상태의 미션 개수 조회
     */
    @Query("SELECT COUNT(um) FROM UserMission um WHERE um.user.id = :userId AND um.groupId = :groupId " +
            "AND um.week = :week AND um.status = :status")
    long countMissionsByUserGroupWeekAndStatus(
            @Param("userId") Long userId,
            @Param("groupId") Long groupId,
            @Param("week") Integer week,
            @Param("status") String status);

    /**
     * 특정 사용자의 특정 그룹에서 오늘 할당된 진행 중인 미션만 조회 (수동 선택 전용)
     */
    @Query("SELECT um FROM UserMission um WHERE " +
            "(:userId IS NULL OR um.user.id = :userId) AND " +
            "um.groupId = :groupId AND um.assignedDate = :date AND " +
            "um.status = 'ing' AND um.week = :week AND um.selectionType = 'manual'")
    List<UserMission> findTodaysInProgressMissionsByUserAndGroup(
            @Param("userId") Long userId,
            @Param("groupId") Long groupId,
            @Param("date") LocalDate date,
            @Param("week") Integer week);

    /**
     * 특정 그룹에서 오늘 특정 미션을 선택한 사용자 수 조회 (수동 선택만)
     */
    @Query("SELECT COUNT(um) FROM UserMission um WHERE um.groupId = :groupId AND " +
            "um.mission.id = :missionId AND um.assignedDate = :date AND " +
            "um.week = :week AND um.selectionType = 'manual'")
    int countTodayMissionSelections(
            @Param("groupId") Long groupId,
            @Param("missionId") Long missionId,
            @Param("date") LocalDate date,
            @Param("week") Integer week);
}