package com.ktb.marong.repository;

import com.ktb.marong.domain.mission.GroupMission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMissionRepository extends JpaRepository<GroupMission, Long> {

    /**
     * 특정 그룹의 특정 주차에 생성된 미션들 조회
     */
    @Query("SELECT gm FROM GroupMission gm JOIN FETCH gm.mission " +
            "WHERE gm.group.id = :groupId AND gm.week = :week")
    List<GroupMission> findByGroupIdAndWeek(@Param("groupId") Long groupId, @Param("week") Integer week);

    /**
     * 특정 그룹, 주차, 미션의 설정 조회
     */
    Optional<GroupMission> findByGroupIdAndMissionIdAndWeek(Long groupId, Long missionId, Integer week);

    /**
     * 특정 그룹의 특정 주차에 특정 미션이 생성되어 있는지 확인
     */
    boolean existsByGroupIdAndMissionIdAndWeek(Long groupId, Long missionId, Integer week);

    /**
     * 특정 그룹, 주차의 생성된 미션 개수 조회
     */
    @Query("SELECT COUNT(gm) FROM GroupMission gm " +
            "WHERE gm.group.id = :groupId AND gm.week = :week")
    long countGroupMissions(@Param("groupId") Long groupId, @Param("week") Integer week);
}