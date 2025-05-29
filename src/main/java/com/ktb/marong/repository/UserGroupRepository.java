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

    // 닉네임 중복 체크 관련 메서드

    /**
     * 특정 그룹 내에서 닉네임 중복 여부 확인 (null 제외)
     */
    @Query("SELECT CASE WHEN COUNT(ug) > 0 THEN true ELSE false END " +
            "FROM UserGroup ug " +
            "WHERE ug.group.id = :groupId " +
            "AND ug.groupUserNickname = :nickname " +
            "AND ug.groupUserNickname IS NOT NULL")
    boolean existsByGroupIdAndGroupUserNickname(@Param("groupId") Long groupId, @Param("nickname") String nickname);

    /**
     * 특정 그룹 내에서 닉네임 중복 여부 확인 (자신 제외, null 제외)
     */
    @Query("SELECT CASE WHEN COUNT(ug) > 0 THEN true ELSE false END " +
            "FROM UserGroup ug " +
            "WHERE ug.group.id = :groupId " +
            "AND ug.groupUserNickname = :nickname " +
            "AND ug.groupUserNickname IS NOT NULL " +
            "AND ug.user.id != :userId")
    boolean existsByGroupIdAndGroupUserNicknameExcludingUser(
            @Param("groupId") Long groupId,
            @Param("nickname") String nickname,
            @Param("userId") Long userId);

    /**
     * 특정 그룹의 모든 닉네임 목록 조회 (중복 체크용, null 제외)
     */
    @Query("SELECT ug.groupUserNickname FROM UserGroup ug " +
            "WHERE ug.group.id = :groupId " +
            "AND ug.groupUserNickname IS NOT NULL")
    List<String> findAllNicknamesByGroupId(@Param("groupId") Long groupId);
}