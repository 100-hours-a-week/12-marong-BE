package com.ktb.marong.dto.response.group;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ktb.marong.domain.group.UserGroup;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupResponseDto {
    private Long groupId;
    private String groupName;
    private String description;
    private String groupImageUrl;
    private int memberCount;
    private String myNickname;
    private String myProfileImageUrl;
    @JsonIgnore
    private boolean isOwner;
    private LocalDateTime joinedAt;

    @JsonProperty("isOwner")
    public boolean isOwner() { return isOwner; }

    public static GroupResponseDto fromUserGroup(UserGroup userGroup, int memberCount) {
        return GroupResponseDto.builder()
                .groupId(userGroup.getGroup().getId())
                .groupName(userGroup.getGroup().getName())
                .description(userGroup.getGroup().getDescription())
                .groupImageUrl(userGroup.getGroup().getImageUrl())
                .memberCount(memberCount)
                .myNickname(userGroup.getGroupUserNickname())
                .myProfileImageUrl(userGroup.getGroupUserProfileImageUrl())
                .isOwner(userGroup.getIsOwner())
                .joinedAt(userGroup.getJoinedAt())
                .build();
    }
}