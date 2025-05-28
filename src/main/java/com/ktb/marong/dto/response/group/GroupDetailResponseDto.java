package com.ktb.marong.dto.response.group;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupDetailResponseDto {
    private Long groupId;
    private String groupName;
    private String description;
    private String imageUrl;
    private String inviteCode;
    private int currentMemberCount;
    private int maxMemberCount;
    private String myGroupUserNickname;
    private String myGroupUserProfileImageUrl;
    private boolean isOwner;
    private LocalDateTime joinedAt;
}