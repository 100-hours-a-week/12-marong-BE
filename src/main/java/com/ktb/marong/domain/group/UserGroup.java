package com.ktb.marong.domain.group;

import com.ktb.marong.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "UserGroups", uniqueConstraints = {
        @UniqueConstraint(name = "uq_user_group", columnNames = {"user_id", "group_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    @Column(name = "group_user_nickname")
    private String groupUserNickname;

    @Column(name = "group_user_profile_image_url")
    private String groupUserProfileImageUrl;

    @CreationTimestamp
    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Column(name = "is_owner")
    private Boolean isOwner = false;

    @Builder
    public UserGroup(User user, Group group, String groupUserNickname,
                     String groupUserProfileImageUrl, Boolean isOwner) {
        this.user = user;
        this.group = group;
        this.groupUserNickname = groupUserNickname;
        this.groupUserProfileImageUrl = groupUserProfileImageUrl;
        this.isOwner = isOwner != null ? isOwner : false;
    }

    /**
     * 그룹 내 사용자 프로필 정보 업데이트
     */
    public void updateGroupUserProfile(String groupUserNickname, String groupUserProfileImageUrl) {
        this.groupUserNickname = groupUserNickname;
        this.groupUserProfileImageUrl = groupUserProfileImageUrl;
    }

    /**
     * 그룹 내 사용자 닉네임이 설정되어 있는지 확인
     */
    public boolean hasGroupUserNickname() {
        return this.groupUserNickname != null && !this.groupUserNickname.trim().isEmpty();
    }
}