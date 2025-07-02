package com.ktb.marong.dto.response.mission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SelectMissionResponseDto {
    private Long missionId;
    private String title;
    private String description;
    private String difficulty;
    private Long groupId;
    private LocalDate selectedDate;
    private LocalDateTime selectedAt;
    private String message;
}