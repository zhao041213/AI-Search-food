package com.example.food.user.character.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record KitchenCharacterNamesRequest(
        @NotNull(message = "人物名称不能为空")
        Map<String, String> names
) {
}
