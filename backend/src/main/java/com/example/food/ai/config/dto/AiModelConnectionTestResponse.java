package com.example.food.ai.config.dto;

public record AiModelConnectionTestResponse(
        boolean connected,
        String provider,
        String protocol,
        String modelName
) {
}
