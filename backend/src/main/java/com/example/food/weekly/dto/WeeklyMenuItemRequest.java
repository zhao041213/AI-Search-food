package com.example.food.weekly.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.util.Locale;

public record WeeklyMenuItemRequest(
        @NotNull
        LocalDate menuDate,

        @NotNull
        @Pattern(regexp = "BREAKFAST|LUNCH|DINNER")
        String mealType,

        @NotNull
        Long recipeId
) {
    public WeeklyMenuItemRequest {
        mealType = mealType == null ? null : mealType.trim().toUpperCase(Locale.ROOT);
    }
}
