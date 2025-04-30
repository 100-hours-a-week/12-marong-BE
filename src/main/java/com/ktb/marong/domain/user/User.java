package com.ktb.marong.domain.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * User 엔티티
 * 마롱 서비스의 사용자 정보를 관리하는 엔티티
 */
@Entity
@Table(name = "Users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "provider_id", nullable = false, unique = true)
    private String providerId;

    @Column(nullable = false)
    private String nickname;

    @Column(name = "provider_name")
    private String providerName;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Column
    private String status = "active";

    @Column(name = "has_completed_survey")
    private Boolean hasCompletedSurvey = false;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public User(String email, String providerId, String nickname, String providerName, String profileImageUrl) {
        this.email = email;
        this.providerId = providerId;
        this.nickname = nickname;
        this.providerName = providerName;
        this.profileImageUrl = profileImageUrl;
    }

    /**
     * 설문 완료 처리
     */
    public void completeInitialSurvey() {
        this.hasCompletedSurvey = true;
    }

    /**
     * 프로필 업데이트
     */
    public void updateProfile(String nickname, String profileImageUrl) {
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
    }

    /**
     * 소프트 삭제 처리
     */
    public void softDelete() {
        this.status = "deleted";
        this.deletedAt = LocalDateTime.now();
    }
}