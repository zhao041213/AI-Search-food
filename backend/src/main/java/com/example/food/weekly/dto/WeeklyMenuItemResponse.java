package com.example.food.weekly.dto;

import java.time.LocalDate;

public record WeeklyMenuItemResponse(
        Long id,
        LocalDate menuDate,
        String mealType,
        Long recipeId,
        String recipeTitle
) {
}
