package com.example.food.stats.dto;

import java.time.LocalDateTime;

public record HotIngredientItemResponse(
        int rank,
        String name,
        long searchCount,
        double share,
        LocalDateTime latestSearchAt,
        String imageUrl,
        String imageStatus
) {

    public HotIngredientItemResponse(
            int rank,
            String name,
            long searchCount,
            double share,
            LocalDateTime latestSearchAt
    ) {
        this(rank, name, searchCount, share, latestSearchAt, null, "PENDING");
    }
}
