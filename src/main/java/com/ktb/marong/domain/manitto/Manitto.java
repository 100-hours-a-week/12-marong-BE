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
    @JoinColumn(name = "giver_id", nullable = false)
    private User giver;  // 마니띠 (현재 사용자)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;  // 마니또 (대상 사용자)

    @Column(name = "week", nullable = false)
    private Integer week;

    @Builder
    public Manitto(Long groupId, User giver, User receiver, Integer week) {
        this.groupId = groupId;
        this.giver = giver;
        this.receiver = receiver;
        this.week = week;
    }
}