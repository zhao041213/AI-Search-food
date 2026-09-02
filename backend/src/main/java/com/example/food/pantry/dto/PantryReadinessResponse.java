package com.example.food.pantry.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PantryReadinessResponse(
        int itemCount,
        int readyCount,
        int shortageCount,
        int expiringSoonCount,
        int readinessPercent,
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
            boolean shortage,
            boolean expiringSoon,
            List<Batch> batches
    ) {
    }

    public record Batch(
            Long pantryItemId,
            BigDecimal quantity,
            String unit,
            LocalDate expireDate
    ) {
    }
}
