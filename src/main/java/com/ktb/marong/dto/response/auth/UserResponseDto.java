package com.ktb.marong.dto.response.auth;

import com.ktb.marong.domain.user.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {
    private Long userId;
    private String kakaoName;
    private String profileImage;
    private String nickname;
    private boolean hasCompletedSurvey;
    private boolean hasGroupNickname; // 추가: 그룹 내 닉네임 설정 여부

    /**
     * User 엔티티로부터 UserResponseDto 생성
     */
    public static UserResponseDto fromEntity(User user, boolean hasGroupNickname) {
        return UserResponseDto.builder()
                .userId(user.getId())
                .kakaoName(user.getNickname())
                .profileImage(user.getProfileImageUrl())
                .nickname(user.getNickname())
                .hasCompletedSurvey(user.getHasCompletedSurvey())
                .hasGroupNickname(hasGroupNickname)
                .build();
    }

    /**
     * User 엔티티로부터 UserResponseDto 생성 (기존 호환성 유지)
     */
    public static UserResponseDto fromEntity(User user) {
        return fromEntity(user, false);
    }
}