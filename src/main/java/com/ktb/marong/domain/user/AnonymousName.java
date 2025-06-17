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

    @Column(name = "group_id", nullable = false)
    private Long groupId; // 그룹 ID를 파라미터로 받도록 수정

    @Column(name = "anonymous_name", nullable = false)
    private String anonymousName;

    @Column(name = "week", nullable = false)
    private Integer week;

    @Builder
    public AnonymousName(User user, Long groupId, String anonymousName, Integer week) {
        this.user = user;
        this.groupId = groupId; // 파라미터로 받은 groupId 사용
        this.anonymousName = anonymousName;
        this.week = week;
    }
}