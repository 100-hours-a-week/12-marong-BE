package com.ktb.marong.domain.feed;

import com.ktb.marong.domain.mission.Mission;
import com.ktb.marong.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;

@Entity
@Table(name = "Posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE Posts SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // MVP에서는 모든 사용자가 하나의 그룹에 속하므로 group_id 필드는 고정값 사용
    @Column(name = "group_id", nullable = false)
    private Long groupId = 1L; // 기본 그룹 ID는 1로 고정

    // 주차 정보 필드 추가
    @Column(name = "week", nullable = false)
    private Integer week;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id", nullable = false)
    private Mission mission;

    @Column(name = "anonymous_snapshot_name", nullable = false)
    private String anonymousSnapshotName;

    @Column(name = "manittee_name")
    private String manitteeName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "image_url")
    private String imageUrl;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public Post(User user, Mission mission, Integer week, String anonymousSnapshotName,
                String manitteeName, String content, String imageUrl) {
        this.user = user;
        this.mission = mission;
        this.week = week;
        this.anonymousSnapshotName = anonymousSnapshotName;
        this.manitteeName = manitteeName;
        this.content = content;
        this.imageUrl = imageUrl;
    }

    public void setId(Long id) {
        this.id = id;
    }
}