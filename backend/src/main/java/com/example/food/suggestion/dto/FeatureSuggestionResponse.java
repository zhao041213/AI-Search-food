package com.example.food.suggestion.dto;

import java.time.LocalDateTime;
import java.util.List;

public record FeatureSuggestionResponse(
        Long id,
        String type,
        String title,
        String detail,
        String expectedEffect,
        String status,
        String screenshotOriginalName,
        String screenshotContentType,
        Long screenshotSize,
        String screenshotUrl,
        Long duplicateOfId,
        String adminReply,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime lastProcessedAt,
        List<FeatureSuggestionAdditionResponse> additions
) {
}
