package com.ktb.marong.domain.recommendation;

import com.ktb.marong.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "PlaceLikes",
        uniqueConstraints = {
                @UniqueConstraint(name = "unique_user_place_like",
                        columnNames = {"user_id", "place_recommendation_id"})
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaceLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_recommendation_id", nullable = false)
    private PlaceRecommendation placeRecommendation;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Builder
    public PlaceLike(User user, PlaceRecommendation placeRecommendation) {
        this.user = user;
        this.placeRecommendation = placeRecommendation;
    }
}