package com.ktb.marong.domain.group;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "`Groups`") // 백틱으로 감싸서 SQL 예약어 문제 해결
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // 표시용 이름 (공백 유지)

    @Column(name = "normalized_name", nullable = false)
    private String normalizedName; // 중복체크용 이름 (공백 제거, 소문자)

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "invite_code", nullable = false, unique = true)
    private String inviteCode;

    @Column(name = "image_url")
    private String imageUrl;

    @Builder
    public Group(String name, String description, String inviteCode, String imageUrl) {
        this.name = name;
        this.normalizedName = name.trim().replaceAll("\\s+", "").toLowerCase(); // 자동 정규화
        this.description = description;
        this.inviteCode = inviteCode;
        this.imageUrl = imageUrl;
    }
}