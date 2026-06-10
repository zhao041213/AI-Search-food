package com.example.food.ai.config.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiModelConfigUpdateRequest(
        @NotBlank
        @Size(max = 64)
        String provider,
        @NotBlank
        @Size(max = 128)
        String modelName,
        @NotBlank
        @Size(max = 1000)
        String endpoint,
        @Size(max = 1000)
        String apiKey,
        Boolean enabled
) {
}
