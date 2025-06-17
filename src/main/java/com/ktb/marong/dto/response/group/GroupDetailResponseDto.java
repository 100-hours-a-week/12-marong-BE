package com.ktb.marong.dto.response.group;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    private String groupImageUrl;
    private String inviteCode;
    private int currentMemberCount;
    private int maxMemberCount;
    private String myNickname;
    private String myProfileImageUrl;
    @JsonIgnore
    private boolean isOwner;
    private LocalDateTime joinedAt;

    @JsonProperty("isOwner")
    public boolean isOwner() { return isOwner; }
}