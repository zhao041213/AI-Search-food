package com.example.food.admin.dashboard;

import java.time.LocalDate;
import java.time.LocalDateTime;

enum AdminDashboardPeriod {
    SEVEN_DAYS("7d", 7),
    THIRTY_DAYS("30d", 30);

    private final String value;
    private final int days;

    AdminDashboardPeriod(String value, int days) {
        this.value = value;
        this.days = days;
    }

    String value() {
        return value;
    }

    LocalDate startDate(LocalDate today) {
        return today.minusDays(days - 1L);
    }

    LocalDateTime endExclusive(LocalDate today) {
        return today.plusDays(1).atStartOfDay();
    }

    static AdminDashboardPeriod parse(String value) {
        String normalized = value == null || value.isBlank() ? SEVEN_DAYS.value : value.trim();
        for (AdminDashboardPeriod period : values()) {
            if (period.value.equals(normalized)) {
                return period;
            }
        }
        throw new IllegalArgumentException("统计周期仅支持 7d 或 30d");
    }
}
