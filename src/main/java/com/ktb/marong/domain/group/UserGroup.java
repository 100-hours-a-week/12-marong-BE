package com.ktb.marong.domain.group;

import com.ktb.marong.common.util.GroupNicknameValidator;
import com.ktb.marong.domain.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "UserGroups")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class UserGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @Column(name = "group_user_nickname", length = 100)
    private String groupUserNickname;

    // 중복체크용 정규화된 닉네임 (공백제거 + 소문자)
    @Column(name = "normalized_nickname", length = 100)
    private String normalizedNickname;

    @Column(name = "group_user_profile_image_url")
    private String groupUserProfileImageUrl;

    @Column(name = "is_owner", nullable = false)
    @Builder.Default
    private Boolean isOwner = false;

    @CreatedDate
    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    /**
     * 그룹 내 사용자 프로필 이미지만 업데이트
     */
    public void updateGroupUserProfileImage(String groupUserProfileImageUrl) {
        this.groupUserProfileImageUrl = groupUserProfileImageUrl;
    }

    /**
     * 그룹 내 사용자 닉네임만 업데이트
     */
    public void updateGroupUserNickname(String groupUserNickname) {
        this.groupUserNickname = groupUserNickname;
        // 닉네임 변경 시 정규화된 닉네임도 함께 업데이트
        this.normalizedNickname = GroupNicknameValidator.normalizeNicknameForDuplication(groupUserNickname);
    }

    /**
     * 그룹 내 사용자 프로필 정보 전체 업데이트
     */
    public void updateGroupUserProfile(String groupUserNickname, String groupUserProfileImageUrl) {
        this.groupUserNickname = groupUserNickname;
        this.groupUserProfileImageUrl = groupUserProfileImageUrl;
        // 닉네임 변경 시 정규화된 닉네임도 함께 업데이트
        this.normalizedNickname = GroupNicknameValidator.normalizeNicknameForDuplication(groupUserNickname);
    }

    /**
     * 그룹 내 닉네임 설정 여부 확인
     */
    public boolean hasGroupUserNickname() {
        return this.groupUserNickname != null && !this.groupUserNickname.trim().isEmpty();
    }

    /**
     * 엔티티가 저장되기 전에 정규화된 닉네임을 설정
     */
    @PrePersist
    @PreUpdate
    private void setNormalizedNickname() {
        if (this.groupUserNickname != null) {
            this.normalizedNickname = GroupNicknameValidator.normalizeNicknameForDuplication(this.groupUserNickname);
        }
        // joined_at이 null인 경우 현재 시간으로 설정 (CreatedDate 백업)
        if (this.joinedAt == null) {
            this.joinedAt = LocalDateTime.now();
        }
    }
}