package com.example.food.ai.config;

public record AiModelRuntimeConfig(
        String provider,
        String modelName,
        String endpoint,
        String apiKey
) {
}
