package com.example.food.ai.config.dto;

public record AiModelConfigResponse(
        String provider,
        String modelName,
        String purpose,
        String endpoint,
        boolean enabled,
        boolean primaryModel,
        boolean apiKeyConfigured,
        String apiKeyPreview
) {
}
