package com.example.food.admin.dashboard.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AdminDashboardOverviewResponse(
        String period,
        LocalDateTime generatedAt,
        AdminDashboardMetricsResponse metrics,
        List<AdminDashboardDailyTrendResponse> dailyTrend,
        List<AdminDashboardInputSourceResponse> inputSources,
        List<AdminDashboardHotIngredientResponse> hotIngredients
) {
}
