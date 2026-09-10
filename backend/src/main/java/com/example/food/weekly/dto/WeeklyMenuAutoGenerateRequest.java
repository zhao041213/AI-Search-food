package com.example.food.weekly.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.Locale;

public record WeeklyMenuAutoGenerateRequest(
        @NotNull
        LocalDate weekStart,
        boolean overwrite,
        String mode
) {

    public static final String PANTRY = "PANTRY";
    public static final String RANDOM = "RANDOM";

    public WeeklyMenuAutoGenerateRequest(LocalDate weekStart, boolean overwrite) {
        this(weekStart, overwrite, PANTRY);
    }

    public WeeklyMenuAutoGenerateRequest {
        mode = mode == null || mode.isBlank()
                ? PANTRY
                : mode.trim().toUpperCase(Locale.ROOT);
        if (!PANTRY.equals(mode) && !RANDOM.equals(mode)) {
            throw new IllegalArgumentException("不支持的 AI 菜单安排模式");
        }
    }
}
