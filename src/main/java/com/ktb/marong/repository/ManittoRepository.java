package com.ktb.marong.repository;

import com.ktb.marong.domain.manitto.Manitto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ManittoRepository extends JpaRepository<Manitto, Long> {

    /**
     * 특정 사용자(manitto), 그룹, 주차에 해당하는 마니또 정보 조회
     */
    List<Manitto> findByManittoIdAndGroupIdAndWeek(Long manittoId, Long groupId, Integer week);
}