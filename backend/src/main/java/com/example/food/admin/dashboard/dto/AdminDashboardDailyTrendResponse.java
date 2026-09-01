package com.example.food.admin.dashboard.dto;

import java.time.LocalDate;

public record AdminDashboardDailyTrendResponse(
        LocalDate date,
        long generationCount,
        long savedRecipeCount
) {
}
