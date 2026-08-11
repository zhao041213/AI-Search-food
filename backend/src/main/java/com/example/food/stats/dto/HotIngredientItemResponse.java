package com.example.food.stats.dto;

import java.time.LocalDateTime;

public record HotIngredientItemResponse(
        int rank,
        String name,
        long searchCount,
        double share,
        LocalDateTime latestSearchAt
) {
}
