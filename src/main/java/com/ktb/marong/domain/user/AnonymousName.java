package com.ktb.marong.domain.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "AnonymousNames")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnonymousName {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // MVP에서는 모든 사용자가 그룹 ID 1에 속함
    @Column(name = "group_id", nullable = false)
    private Long groupId = 1L;

    @Column(name = "anonymous_name", nullable = false)
    private String anonymousName;

    @Column(name = "week", nullable = false)
    private Integer week;

    @Builder
    public AnonymousName(User user, String anonymousName, Integer week) {
        this.user = user;
        this.anonymousName = anonymousName;
        this.week = week;
    }
}