package com.ktb.marong.domain.recommendation;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "PlaceRecommendations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaceRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private PlaceRecommendationSession session;

    @Column(nullable = false)
    private String type; // "cafe" 또는 "restaurant"

    @Column(nullable = false)
    private String name;

    @Column
    private String category;

    @Column(name = "opening_hours")
    private String openingHours;

    @Builder
    public PlaceRecommendation(PlaceRecommendationSession session, String type, String name,
                               String category, String openingHours) {
        this.session = session;
        this.type = type;
        this.name = name;
        this.category = category;
        this.openingHours = openingHours;
    }
}