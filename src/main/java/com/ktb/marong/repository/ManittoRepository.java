package com.ktb.marong.repository;

import com.ktb.marong.domain.manitto.Manitto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ManittoRepository extends JpaRepository<Manitto, Long> {

    /**
     * 특정 사용자(giver), 그룹, 주차에 해당하는 마니또 정보 조회
     */
    Optional<Manitto> findByGiverIdAndGroupIdAndWeek(Long giverId, Long groupId, Integer week);
}