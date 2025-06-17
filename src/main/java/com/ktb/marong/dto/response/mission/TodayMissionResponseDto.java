package com.ktb.marong.dto.response.mission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TodayMissionResponseDto {
    private Long missionId;
    private String title;
    private String description;
    private String difficulty;
    private LocalDate selectedAt;
    private String status; // "ing", "completed", "incomplete"
}