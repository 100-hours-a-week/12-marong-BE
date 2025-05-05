package com.ktb.marong.common.util;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class WeekCalculator {

    // 누적 주차수 계산하기 위한 기준 날짜 (2025년 1월 1일) (상수로 정의)
    private static final LocalDate SERVICE_START_DATE = LocalDate.of(2025, 1, 1);

    /**
     * 2025.01.01 기준 현재 누적 주차 계산
     */
    public static int getCurrentWeek() {
        LocalDate today = LocalDate.now();
        long weeksBetween = ChronoUnit.WEEKS.between(SERVICE_START_DATE, today) + 1;
        return (int) weeksBetween;
    }

    /**
     * 특정 날짜의 누적 주차 계산
     */
    public static int getWeekOf(LocalDate date) {
        long weeksBetween = ChronoUnit.WEEKS.between(SERVICE_START_DATE, date) + 1;
        return (int) weeksBetween;
    }
}