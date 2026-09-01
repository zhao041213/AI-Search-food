package com.example.food.recipe.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RecommendationReactionRequest(
        @NotBlank(message = "反馈不能为空")
        @Size(max = 16, message = "反馈类型不合法")
        String reaction
) {
}
