package com.example.food.ai.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DotEnvSynchronizerTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void updatesAiSettingsWithoutRemovingUnrelatedEnvValues() throws Exception {
        Path envFile = temporaryDirectory.resolve(".env");
        Files.writeString(envFile, "# keep this comment\nMYSQL_DATABASE=recipe\nAI_TEXT_RECIPE_MODEL=old-model\n",
                StandardCharsets.UTF_8);
        AiModelConfig config = config("qwen", "anthropic", "qwen-plus",
                "https://dashscope.aliyuncs.com/apps/anthropic", "configured-value");

        new DotEnvSynchronizer(envFile).synchronize(config);

        String content = Files.readString(envFile, StandardCharsets.UTF_8);
        assertThat(content).contains("# keep this comment", "MYSQL_DATABASE=recipe")
                .contains("AI_TEXT_RECIPE_PROVIDER=qwen")
                .contains("AI_TEXT_RECIPE_PROTOCOL=anthropic")
                .contains("AI_TEXT_RECIPE_MODEL=qwen-plus")
                .contains("AI_TEXT_RECIPE_ENDPOINT=https://dashscope.aliyuncs.com/apps/anthropic")
                .contains("AI_TEXT_RECIPE_API_KEY=configured-value");
    }

    @Test
    void leavesExistingSecretWhenIncomingSecretIsBlank() throws Exception {
        Path envFile = temporaryDirectory.resolve(".env");
        Files.writeString(envFile, "AI_TEXT_RECIPE_API_KEY=existing-value\n", StandardCharsets.UTF_8);
        AiModelConfig config = config("qwen", "openai", "qwen-plus",
                "https://dashscope.aliyuncun.com/compatible-mode/v1", "");

        new DotEnvSynchronizer(envFile).synchronize(config);

        assertThat(Files.readString(envFile, StandardCharsets.UTF_8))
                .contains("AI_TEXT_RECIPE_API_KEY=existing-value");
    }

    private AiModelConfig config(String provider, String protocol, String model, String endpoint, String apiKey) {
        AiModelConfig config = new AiModelConfig();
        config.setProvider(provider);
        config.setApiProtocol(protocol);
        config.setModelName(model);
        config.setEndpointUrl(endpoint);
        config.setApiKey(apiKey);
        return config;
    }
}
