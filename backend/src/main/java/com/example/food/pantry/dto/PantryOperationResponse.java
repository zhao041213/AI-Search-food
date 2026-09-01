package com.example.food.pantry.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PantryOperationResponse(
        Long id,
        String operationType,
        String sourceType,
        Long sourceId,
        String status,
        Integer actualServings,
        LocalDateTime createdAt,
        LocalDateTime reversedAt,
        List<Item> items,
        boolean undoable
) {
    public record Item(
            Long pantryItemId,
            String ingredientName,
            BigDecimal quantity,
            String unit,
            BigDecimal beforeQuantity,
            BigDecimal afterQuantity,
            String rawAmount
    ) {}
}
