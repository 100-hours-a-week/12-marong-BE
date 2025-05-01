package com.ktb.marong.domain.survey;

import com.ktb.marong.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "SurveyMBTI")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SurveyMBTI {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Column(nullable = false, length = 10)
    private String mbti;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public SurveyMBTI(User user, Integer eiScore, Integer snScore, Integer tfScore, Integer jpScore) {
        this.user = user;
        this.eiScore = eiScore;
        this.snScore = snScore;
        this.tfScore = tfScore;
        this.jpScore = jpScore;
        this.mbti = calculateMbti();
    }

    private String calculateMbti() {
        String mbti = "";
        mbti += (eiScore > 50) ? "E" : "I";
        mbti += (snScore > 50) ? "N" : "S";
        mbti += (tfScore > 50) ? "F" : "T";
        mbti += (jpScore > 50) ? "P" : "J";
        return mbti;
    }
}