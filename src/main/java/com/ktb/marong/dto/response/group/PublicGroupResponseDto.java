package com.ktb.marong.dto.response.group;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ktb.marong.domain.group.Group;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicGroupResponseDto {
    private Long groupId;
    private String groupName;
    private String description;
    private String groupImageUrl;
    private int currentMemberCount;
    private int maxMemberCount;
    @JsonIgnore
    private boolean isJoinable; // 가입 가능 여부 (멤버 수 제한 기준)

    // 명시적인 getter 메소드로 JSON 직렬화 제어
    @JsonProperty("isJoinable")
    public boolean getIsJoinable() {
        return isJoinable;
    }

    public static PublicGroupResponseDto fromGroup(Group group, int currentMemberCount, int maxMemberCount) {
        return PublicGroupResponseDto.builder()
                .groupId(group.getId())
                .groupName(group.getName())
                .description(group.getDescription())
                .groupImageUrl(group.getImageUrl())
                .currentMemberCount(currentMemberCount)
                .maxMemberCount(maxMemberCount)
                .isJoinable(currentMemberCount < maxMemberCount)
                .build();
    }
}