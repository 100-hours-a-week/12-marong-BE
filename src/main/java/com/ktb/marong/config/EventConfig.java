package com.ktb.marong.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Configuration
@Getter
public class EventConfig {

    @Value("${event.unlimited-mission.enabled:false}")
    private boolean unlimitedMissionEventEnabled;

    @Value("${event.unlimited-mission.start-date:}")
    private String unlimitedMissionStartDate;

    @Value("${event.unlimited-mission.end-date:}")
    private String unlimitedMissionEndDate;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 현재 무제한 미션 이벤트 기간인지 확인
     * 7월 7일 12:00부터 7월 13일 23:59까지
     */
    public boolean isUnlimitedMissionEventPeriod() {
        if (!unlimitedMissionEventEnabled) {
            return false;
        }

        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDate startDate = LocalDate.parse(unlimitedMissionStartDate, DATE_FORMATTER);
            LocalDate endDate = LocalDate.parse(unlimitedMissionEndDate, DATE_FORMATTER);

            LocalDateTime eventStart = startDate.atTime(12, 0); // 12:00
            LocalDateTime eventEnd = endDate.atTime(23, 59); // 23:59

            boolean isEventPeriod = !now.isBefore(eventStart) && !now.isAfter(eventEnd);

            if (isEventPeriod) {
                log.debug("현재 무제한 미션 이벤트 기간: {} 12:00 ~ {} 23:59, 현재: {}",
                        unlimitedMissionStartDate, unlimitedMissionEndDate, now);
            }

            return isEventPeriod;
        } catch (Exception e) {
            log.error("이벤트 기간 파싱 오류: startDate={}, endDate={}",
                    unlimitedMissionStartDate, unlimitedMissionEndDate, e);
            return false;
        }
    }

    /**
     * 이벤트 기간 정보 조회
     */
    public String getEventPeriodInfo() {
        if (!unlimitedMissionEventEnabled) {
            return "이벤트가 비활성화되어 있습니다.";
        }

        return String.format("무제한 미션 이벤트 기간: %s ~ %s",
                unlimitedMissionStartDate, unlimitedMissionEndDate);
    }
}