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

    /**
     * User 엔티티로부터 UserResponseDto 생성
     */
    public static UserResponseDto fromEntity(User user) {
        return UserResponseDto.builder()
                .userId(user.getId())
                .kakaoName(user.getNickname())
                .profileImage(user.getProfileImageUrl())
                .nickname(user.getNickname())
                .hasCompletedSurvey(user.getHasCompletedSurvey())
                .build();
    }
}