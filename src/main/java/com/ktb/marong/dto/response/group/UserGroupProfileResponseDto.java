package com.ktb.marong.dto.response.group;

import com.fasterxml.jackson.annotation.JsonFormat;
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
public class UserGroupProfileResponseDto {

    private Long groupId;
    private String groupName;
    private String groupImageUrl;
    private int memberCount;
    private String myNickname;
    private String myProfileImageUrl;
    @JsonIgnore
    private boolean isOwner;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime joinedAt;

    @JsonProperty("isOwner")
    public boolean isOwner() { return isOwner; }
}