package com.ktb.marong.domain.mission;

import com.ktb.marong.domain.group.Group;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "GroupMissions",
        uniqueConstraints = @UniqueConstraint(name = "uq_group_mission_week",
                columnNames = {"group_id", "mission_id", "week"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupMission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id", nullable = false)
    private Mission mission;

    @Column(name = "week", nullable = false)
    private Integer week;

    @Column(name = "max_assignable", nullable = false)
    private Integer maxAssignable = 5;

    @Column(name = "remaining_count", nullable = false)
    private Integer remainingCount = 5;

    @Builder
    public GroupMission(Group group, Mission mission, Integer week,
                        Integer maxAssignable, Integer remainingCount) {
        this.group = group;
        this.mission = mission;
        this.week = week;
        this.maxAssignable = maxAssignable != null ? maxAssignable : 5;
        this.remainingCount = remainingCount != null ? remainingCount : 5;
    }

    /**
     * 미션 선택 시 남은 인원 감소
     */
    public void decreaseRemainingCount() {
        if (this.remainingCount > 0) {
            this.remainingCount--;
        }
    }

    /**
     * 미션 선택 취소 시 남은 인원 증가
     */
    public void increaseRemainingCount() {
        if (this.remainingCount < this.maxAssignable) {
            this.remainingCount++;
        }
    }

    /**
     * 선택 가능한지 확인
     */
    public boolean isSelectable() {
        return this.remainingCount > 0;
    }
}