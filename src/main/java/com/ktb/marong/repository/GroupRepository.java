package com.ktb.marong.repository;

import com.ktb.marong.domain.group.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {
    // 기본적인 CRUD 메서드는 JpaRepository에서 제공됨
    // MVP 단계에서는 추가 쿼리 메서드는 필요하지 않음
}