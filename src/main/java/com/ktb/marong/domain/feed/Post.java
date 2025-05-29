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

    @Column(name = "group_id", nullable = false)
    private Long groupId; // 그룹 ID를 파라미터로 받도록 수정

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
    public Post(User user, Long groupId, Mission mission, Integer week, String anonymousSnapshotName,
                String manitteeName, String content, String imageUrl) {
        this.user = user;
        this.groupId = groupId; // 파라미터로 받은 groupId 사용
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