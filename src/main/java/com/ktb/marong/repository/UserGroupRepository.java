package com.ktb.marong.repository;

import com.ktb.marong.domain.group.UserGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserGroupRepository extends JpaRepository<UserGroup, Long> {

    /**
     * 특정 사용자가 속한 그룹 개수 조회
     */
    int countByUserId(Long userId);

    /**
     * 특정 사용자가 특정 그룹에 속해있는지 확인
     */
    boolean existsByUserIdAndGroupId(Long userId, Long groupId);

    /**
     * 특정 사용자가 속한 모든 그룹 조회 (그룹 정보 포함)
     */
    @Query("SELECT ug FROM UserGroup ug JOIN FETCH ug.group WHERE ug.user.id = :userId")
    List<UserGroup> findByUserIdWithGroup(@Param("userId") Long userId);

    /**
     * 특정 사용자와 그룹의 관계 조회
     */
    @Query("SELECT ug FROM UserGroup ug JOIN FETCH ug.group WHERE ug.user.id = :userId AND ug.group.id = :groupId")
    Optional<UserGroup> findByUserIdAndGroupId(@Param("userId") Long userId, @Param("groupId") Long groupId);

    /**
     * 특정 그룹의 멤버 수 조회
     */
    int countByGroupId(Long groupId);

    /**
     * 특정 그룹의 모든 멤버 조회
     */
    @Query("SELECT ug FROM UserGroup ug JOIN FETCH ug.user WHERE ug.group.id = :groupId")
    List<UserGroup> findByGroupIdWithUser(@Param("groupId") Long groupId);

    /**
     * 특정 사용자가 생성한 그룹들 조회
     */
    @Query("SELECT ug FROM UserGroup ug JOIN FETCH ug.group WHERE ug.user.id = :userId AND ug.isOwner = true")
    List<UserGroup> findOwnedGroupsByUserId(@Param("userId") Long userId);
}