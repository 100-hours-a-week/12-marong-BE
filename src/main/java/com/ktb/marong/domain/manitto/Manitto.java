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
    @JoinColumn(name = "manittee_id", nullable = false) // giver_id에서 변경
    private User manittee;  // 현재 사용자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manitto_id", nullable = false) // receiver_id에서 변경
    private User manitto;  // 대상 사용자

    @Column(name = "week", nullable = false)
    private Integer week;

    @Builder
    public Manitto(Long groupId, User manittee, User manitto, Integer week) {
        this.groupId = groupId;
        this.manittee = manittee;
        this.manitto = manitto;
        this.week = week;
    }
}