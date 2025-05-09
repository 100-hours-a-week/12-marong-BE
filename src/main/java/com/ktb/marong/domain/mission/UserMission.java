package com.ktb.marong.domain.mission;

import com.ktb.marong.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
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

    @Column(name = "assigned_date", nullable = false)
    private LocalDate assignedDate;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public UserMission(User user, Long groupId, Mission mission, Integer week, LocalDate assignedDate) {
        this.user = user;
        this.groupId = groupId;
        this.mission = mission;
        this.week = week;
        this.assignedDate = assignedDate != null ? assignedDate : LocalDate.now();
    }

    /**
     * 미션 완료 처리
     */
    public void complete() {
        this.status = "completed";
    }

    /**
     * 미션 미완료 처리
     * 하루가 지났지만 완료하지 못한 미션에 대한 상태 갱신
     */
    public void markAsIncomplete() {
        this.status = "incomplete";
    }

    /**
     * 미션이 진행 중인지 확인
     */
    public boolean isInProgress() {
        return "ing".equals(this.status);
    }

    /**
     * 미션이 완료되었는지 확인
     */
    public boolean isCompleted() {
        return "completed".equals(this.status);
    }
}