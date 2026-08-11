package com.example.food.stats.dto;

import java.time.LocalDateTime;
import java.util.List;

public record HotIngredientStatsResponse(
        String period,
        long totalSearches,
        long totalIngredientOccurrences,
        LocalDateTime generatedAt,
        List<HotIngredientItemResponse> items
) {
}
