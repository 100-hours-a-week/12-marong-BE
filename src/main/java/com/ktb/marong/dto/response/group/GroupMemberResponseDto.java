package com.ktb.marong.dto.response.group;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 그룹 멤버 조회 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMemberResponseDto {

    private Long userId;                  // 사용자 ID
    private String nickname;              // 표시될 닉네임 (그룹 닉네임 또는 카카오 실명)
    private String profileImageUrl;       // 그룹 프로필 사진 URL (없으면 null)

    @JsonIgnore
    private boolean isOwner;              // 그룹 생성자 여부

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime joinedAt;       // 그룹 가입일시

    @JsonProperty("isOwner")
    public boolean isOwner() {
        return isOwner;
    }
}