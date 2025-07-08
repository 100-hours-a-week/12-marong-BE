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

    // 기존 닉네임 중복 체크 관련 메서드 (정규화된 닉네임 기준)

    /**
     * 특정 그룹 내에서 정규화된 닉네임 중복 여부 확인 (null 제외)
     */
    @Query("SELECT CASE WHEN COUNT(ug) > 0 THEN true ELSE false END " +
            "FROM UserGroup ug " +
            "WHERE ug.group.id = :groupId " +
            "AND ug.normalizedNickname = :normalizedNickname " +
            "AND ug.normalizedNickname IS NOT NULL")
    boolean existsByGroupIdAndNormalizedNickname(@Param("groupId") Long groupId, @Param("normalizedNickname") String normalizedNickname);

    /**
     * 특정 그룹 내에서 정규화된 닉네임 중복 여부 확인 (자신 제외, null 제외)
     */
    @Query("SELECT CASE WHEN COUNT(ug) > 0 THEN true ELSE false END " +
            "FROM UserGroup ug " +
            "WHERE ug.group.id = :groupId " +
            "AND ug.normalizedNickname = :normalizedNickname " +
            "AND ug.normalizedNickname IS NOT NULL " +
            "AND ug.user.id != :userId")
    boolean existsByGroupIdAndNormalizedNicknameExcludingUser(
            @Param("groupId") Long groupId,
            @Param("normalizedNickname") String normalizedNickname,
            @Param("userId") Long userId);

    // 방어로직 메소드들 - null 상태인 normalized_nickname 처리

    /**
     * normalized_nickname이 null인 사용자들 중에서 실시간 정규화해서 중복체크
     * 방어로직용 메소드
     */
    @Query("SELECT COUNT(ug) > 0 FROM UserGroup ug " +
            "WHERE ug.group.id = :groupId " +
            "AND ug.normalizedNickname IS NULL " +
            "AND ug.groupUserNickname IS NOT NULL " +
            "AND LOWER(REPLACE(REPLACE(REPLACE(TRIM(ug.groupUserNickname), ' ', ''), CHAR(9), ''), CHAR(10), '')) = :normalizedNickname")
    boolean existsByGroupIdAndNullNormalizedNicknameWithRuntimeNormalization(@Param("groupId") Long groupId,
                                                                             @Param("normalizedNickname") String normalizedNickname);

    /**
     * 닉네임 중복체크 - 방어로직 포함 (사용자 제외)
     */
    @Query("SELECT COUNT(ug) > 0 FROM UserGroup ug " +
            "WHERE ug.group.id = :groupId " +
            "AND ug.user.id != :excludeUserId " +
            "AND (ug.normalizedNickname = :normalizedNickname " +
            "OR (ug.normalizedNickname IS NULL AND ug.groupUserNickname IS NOT NULL " +
            "AND LOWER(REPLACE(REPLACE(REPLACE(TRIM(ug.groupUserNickname), ' ', ''), CHAR(9), ''), CHAR(10), '')) = :normalizedNickname))")
    boolean existsByGroupIdAndNormalizedNicknameExcludingUserWithFallback(@Param("groupId") Long groupId,
                                                                          @Param("normalizedNickname") String normalizedNickname,
                                                                          @Param("excludeUserId") Long excludeUserId);

    /**
     * 닉네임 중복체크 - 방어로직 포함 (전체)
     */
    @Query("SELECT COUNT(ug) > 0 FROM UserGroup ug " +
            "WHERE ug.group.id = :groupId " +
            "AND (ug.normalizedNickname = :normalizedNickname " +
            "OR (ug.normalizedNickname IS NULL AND ug.groupUserNickname IS NOT NULL " +
            "AND LOWER(REPLACE(REPLACE(REPLACE(TRIM(ug.groupUserNickname), ' ', ''), CHAR(9), ''), CHAR(10), '')) = :normalizedNickname))")
    boolean existsByGroupIdAndNormalizedNicknameWithFallback(@Param("groupId") Long groupId,
                                                             @Param("normalizedNickname") String normalizedNickname);

    /**
     * 특정 그룹의 모든 닉네임 목록 조회 (표시용 닉네임 반환, null 제외)
     */
    @Query("SELECT ug.groupUserNickname FROM UserGroup ug " +
            "WHERE ug.group.id = :groupId " +
            "AND ug.groupUserNickname IS NOT NULL " +
            "ORDER BY ug.joinedAt ASC")
    List<String> findAllNicknamesByGroupId(@Param("groupId") Long groupId);
}