package com.example.food.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record FinishedDishReviewRequest(
        Long recipeId,
        @NotBlank(message = "菜谱标题不能为空") @Size(max = 120, message = "菜谱标题不能超过 120 个字符") String recipeTitle,
        @Size(max = 30, message = "食材数量不能超过 30 项") List<@NotBlank @Size(max = 100) String> ingredients,
        @Size(max = 20, message = "步骤数量不能超过 20 项") List<@NotBlank @Size(max = 300) String> steps
) {
}
