package com.ktb.marong.dto.response.group;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateGroupResponseDto {
    private Long groupId;
    private String groupName;
    private String inviteCode;
}