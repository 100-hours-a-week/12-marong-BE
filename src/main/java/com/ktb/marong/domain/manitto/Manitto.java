package com.ktb.marong.domain.manitto;

import com.ktb.marong.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Manittos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Manitto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manitto_id", nullable = false) // 미션을 수행하는 사람
    private User manitto;  // 현재 사용자 (미션 수행자)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manittee_id", nullable = false) // 미션을 받는 사람
    private User manittee;  // 대상 사용자 (미션 받는 사람)

    @Column(name = "week", nullable = false)
    private Integer week;

    @Builder
    public Manitto(Long groupId, User manitto, User manittee, Integer week) {
        this.groupId = groupId;
        this.manitto = manitto;
        this.manittee = manittee;
        this.week = week;
    }
}