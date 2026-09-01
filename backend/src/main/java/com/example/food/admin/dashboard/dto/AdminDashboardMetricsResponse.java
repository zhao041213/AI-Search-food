package com.example.food.admin.dashboard.dto;

public record AdminDashboardMetricsResponse(
        long newUserCount,
        long generationCount,
        long savedRecipeCount,
        long reviewCount
) {
}
