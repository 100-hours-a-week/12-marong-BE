package com.ktb.marong.dto.request.mission;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SelectMissionRequestDto {

    @NotNull(message = "미션 ID는 필수입니다.")
    private Long missionId;

    @NotNull(message = "그룹 ID는 필수입니다.")
    private Long groupId;
}