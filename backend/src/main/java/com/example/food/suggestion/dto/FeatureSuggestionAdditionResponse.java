package com.example.food.suggestion.dto;

import java.time.LocalDateTime;

public record FeatureSuggestionAdditionResponse(
        Long id,
        Long userId,
        String content,
        LocalDateTime createdAt
) {
}
