package com.example.food.shopping.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ShoppingItemCheckRequest(
        @NotNull
        Long searchLogId,
        @NotBlank
        @Size(max = 128)
        String ingredientName,
        @Size(max = 24)
        String status,
        Boolean checked
) {

    public ShoppingItemCheckRequest(Long searchLogId, String ingredientName, String status) {
        this(searchLogId, ingredientName, status, null);
    }

    public ShoppingItemCheckRequest(Long searchLogId, String ingredientName, boolean checked) {
        this(searchLogId, ingredientName, checked ? "READY" : "PENDING", checked);
    }
}
