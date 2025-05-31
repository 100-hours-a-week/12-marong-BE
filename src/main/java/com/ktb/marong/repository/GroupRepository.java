package com.ktb.marong.repository;

import com.ktb.marong.domain.group.Group;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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

    /**
     * 모든 그룹을 생성일시 기준 최신순으로 조회 (페이지네이션 지원)
     */
    @Query("SELECT g FROM Group g ORDER BY g.id DESC")
    Page<Group> findAllOrderByIdDesc(Pageable pageable);
}