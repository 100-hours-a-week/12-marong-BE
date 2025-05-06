package com.ktb.marong.domain.recommendation;

import com.ktb.marong.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "PlaceRecommendationSessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaceRecommendationSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manittee_id", nullable = false)
    private User manittee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manitto_id", nullable = false)
    private User manitto;

    @Column(name = "week", nullable = false)
    private Integer week;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Builder
    public PlaceRecommendationSession(User manittee, User manitto, Integer week) {
        this.manittee = manittee;
        this.manitto = manitto;
        this.week = week;
        this.createdAt = LocalDateTime.now();
    }
}