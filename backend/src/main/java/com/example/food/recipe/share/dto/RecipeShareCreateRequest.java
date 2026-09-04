package com.example.food.recipe.share.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RecipeShareCreateRequest(
        @NotBlank(message = "分享有效期不能为空")
        @Pattern(regexp = "1|7|30|PERMANENT", message = "分享有效期不合法")
        String validity
) {
}
