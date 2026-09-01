package com.example.food.pantry.dto;

import java.math.BigDecimal;
import java.util.List;

public record CookingPreviewResponse(
        Long recipeId,
        String recipeTitle,
        int defaultServings,
        int actualServings,
        int itemCount,
        int enoughCount,
        int pendingCount,
        List<Item> items
) {
    public record Item(
            String ingredientName,
            String rawAmount,
            BigDecimal expectedQuantity,
            String expectedUnit,
            BigDecimal availableQuantity,
            String availableUnit,
            String status,
            String message,
            List<Batch> batches
    ) {}

    public record Batch(Long pantryItemId, BigDecimal quantity, String unit, java.time.LocalDate expireDate) {}
}
