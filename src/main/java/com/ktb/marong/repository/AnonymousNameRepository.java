package com.ktb.marong.repository;

import com.ktb.marong.domain.user.AnonymousName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AnonymousNameRepository extends JpaRepository<AnonymousName, Long> {

    @Query("SELECT a.anonymousName FROM AnonymousName a WHERE a.user.id = :userId AND a.groupId = 1")
    Optional<String> findAnonymousNameByUserId(@Param("userId") Long userId);
}