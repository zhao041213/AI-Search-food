package com.example.food.recipe.dto;

import java.time.LocalDateTime;

public record RecentSearchResponse(
        Long id,
        String ingredients,
        String mealType,
        String goal,
        String searchMode,
        LocalDateTime createdAt
) {
}
