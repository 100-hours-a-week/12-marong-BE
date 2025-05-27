package com.ktb.marong.repository;

import com.ktb.marong.domain.group.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

    /**
     * 그룹 이름으로 그룹 조회
     */
    Optional<Group> findByName(String name);

    /**
     * 초대 코드로 그룹 조회
     */
    Optional<Group> findByInviteCode(String inviteCode);

    /**
     * 그룹 이름 중복 확인
     */
    boolean existsByName(String name);

    /**
     * 초대 코드 중복 확인
     */
    boolean existsByInviteCode(String inviteCode);
}