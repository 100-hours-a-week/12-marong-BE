package com.ktb.marong.domain.mission;

import com.ktb.marong.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "UserMissions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserMission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "group_id", nullable = false)
    private Long groupId = 1L;  // MVP에서는 기본 그룹 ID를 1로 고정

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id", nullable = false)
    private Mission mission;

    @Column(name = "status", nullable = false)
    private String status = "ing";  // 기본값: ing (진행 중)

    @Column(name = "week", nullable = false)
    private Integer week;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public UserMission(User user, Long groupId, Mission mission, Integer week) {
        this.user = user;
        this.groupId = groupId;
        this.mission = mission;
        this.week = week;
    }

    /**
     * 미션 완료 처리
     */
    public void complete() {
        this.status = "completed";
    }
}