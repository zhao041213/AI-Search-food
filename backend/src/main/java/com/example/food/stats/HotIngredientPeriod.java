package com.example.food.stats;

import java.time.LocalDateTime;

public enum HotIngredientPeriod {
    ALL("all", 0),
    SEVEN_DAYS("7d", 7),
    THIRTY_DAYS("30d", 30);

    private final String value;
    private final int days;

    HotIngredientPeriod(String value, int days) {
        this.value = value;
        this.days = days;
    }

    public String value() {
        return value;
    }

    public LocalDateTime cutoff(LocalDateTime now) {
        return days == 0 ? null : now.minusDays(days);
    }

    public static HotIngredientPeriod parse(String value) {
        String normalized = value == null || value.isBlank() ? "all" : value.trim();
        for (HotIngredientPeriod period : values()) {
            if (period.value.equals(normalized)) {
                return period;
            }
        }
        throw new IllegalArgumentException("统计周期仅支持 all、7d、30d");
    }
}
