package com.example.food.ai.config;

public record AiModelRuntimeConfig(
        String provider,
        String protocol,
        String modelName,
        String endpoint,
        String apiKey
) {

    public AiModelRuntimeConfig(String provider, String modelName, String endpoint, String apiKey) {
        this(provider, "openai", modelName, endpoint, apiKey);
    }
}
