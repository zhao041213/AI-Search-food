package com.example.food.weekly.dto;

public record WeeklyShoppingItemResponse(
        String ingredientName,
        String amount,
        boolean alreadyOwned,
        String status
) {
}
