package com.ktb.marong.dto.response.manitto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MissionStatusResponseDto {
    private ProgressDto progress;
    private MissionsDto missions;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProgressDto {
        private int completed;
        private int incomplete; // 추가된 미완료 미션 개수 필드
        private int total;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MissionsDto {
        private List<MissionDto> inProgress;
        private List<MissionDto> completed;
        private List<MissionDto> incomplete;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MissionDto {
        private Long missionId;
        private String title;
        private String description;
        private String difficulty;
    }
}