package com.example.food.weekly.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record WeeklyMenuAutoGenerateRequest(
        @NotNull
        LocalDate weekStart,
        boolean overwrite
) {
}
