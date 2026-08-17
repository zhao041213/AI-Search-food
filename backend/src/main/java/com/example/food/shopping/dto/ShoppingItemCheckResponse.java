package com.example.food.shopping.dto;

public record ShoppingItemCheckResponse(
        String ingredientName,
        String status,
        boolean checked
) {

    public ShoppingItemCheckResponse(String ingredientName, boolean checked) {
        this(ingredientName, checked ? "READY" : "PENDING", checked);
    }
}
