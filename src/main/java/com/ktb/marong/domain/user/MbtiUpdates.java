package com.ktb.marong.domain.user;

import com.ktb.marong.domain.feed.Post;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "MbtiUpdates")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MbtiUpdates {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "ei_score", nullable = false)
    private Integer eiScore;

    @Column(name = "sn_score", nullable = false)
    private Integer snScore;

    @Column(name = "tf_score", nullable = false)
    private Integer tfScore;

    @Column(name = "jp_score", nullable = false)
    private Integer jpScore;

    @Column(name = "changed_mbti_type", nullable = false, length = 2)
    private String changedMbtiType;

    @Column(name = "change_reason", nullable = false, columnDefinition = "TEXT")
    private String changeReason;

    @Column(name = "previous_score", nullable = false)
    private Integer previousScore;

    @Column(name = "current_score", nullable = false)
    private Integer currentScore;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Builder
    public MbtiUpdates(Post post, User user, Integer eiScore, Integer snScore,
                       Integer tfScore, Integer jpScore, String changedMbtiType,
                       String changeReason, Integer previousScore, Integer currentScore) {
        this.post = post;
        this.user = user;
        this.eiScore = eiScore;
        this.snScore = snScore;
        this.tfScore = tfScore;
        this.jpScore = jpScore;
        this.changedMbtiType = changedMbtiType;
        this.changeReason = changeReason;
        this.previousScore = previousScore;
        this.currentScore = currentScore;
    }
}