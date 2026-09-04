package com.example.food.recipe.collection.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SavedRecipeBatchDeleteRequest(
        @NotEmpty(message = "请选择至少一道菜谱")
        @Size(max = 100, message = "单次最多操作100道菜谱")
        List<Long> recipeIds
) {
}
