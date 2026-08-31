package com.example.food.weekly.dto;

import java.time.LocalDate;
import java.util.List;

public record WeeklyMenuResponse(
        Long id,
        LocalDate weekStart,
        LocalDate weekEnd,
        List<WeeklyMenuItemResponse> items,
        List<WeeklyShoppingItemResponse> shoppingItems
) {
    public WeeklyMenuResponse {
        items = items == null ? List.of() : List.copyOf(items);
        shoppingItems = shoppingItems == null ? List.of() : List.copyOf(shoppingItems);
    }
}
