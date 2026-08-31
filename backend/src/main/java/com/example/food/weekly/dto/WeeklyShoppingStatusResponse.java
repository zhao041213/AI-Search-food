package com.example.food.weekly.dto;

public record WeeklyShoppingStatusResponse(
        String ingredientName,
        String amount,
        boolean alreadyOwned,
        String status
) {
}
