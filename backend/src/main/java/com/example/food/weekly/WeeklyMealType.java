package com.example.food.weekly;

import java.util.Locale;

public enum WeeklyMealType {
    BREAKFAST,
    LUNCH,
    DINNER;

    public static WeeklyMealType from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("餐次不能为空");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("餐次不合法");
        }
    }
}
