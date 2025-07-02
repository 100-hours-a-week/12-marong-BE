package com.ktb.marong.dto.response.mission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableMissionResponseDto {
    private Long groupId;
    private String groupName;
    private String date;
    private boolean canSelectToday; // 오늘 미션을 선택할 수 있는지 여부
    private MissionSelectionStatus todaySelection; // 오늘 선택한 미션 정보
    private List<AvailableMissionDto> availableMissions;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AvailableMissionDto {
        private Long missionId;
        private String title;
        private String description;
        private String difficulty;
        private int currentSelections; // 오늘 현재까지 선택한 사람 수
        private int maxSelections; // 최대 선택 가능 인원 (5명)
        private int remainingSelections; // 남은 선택 가능 인원
        private boolean alreadySelectedInWeek; // 이번 주차에 이미 선택했는지 여부
        private boolean selectable; // 선택 가능 여부
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MissionSelectionStatus {
        private Long missionId;
        private String title;
        private String description;
        private String difficulty;
        private String selectedAt;
    }
}