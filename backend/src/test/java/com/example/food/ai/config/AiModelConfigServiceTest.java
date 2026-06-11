package com.example.food.ai.config;

import com.example.food.ai.qwen.QwenProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiModelConfigServiceTest {

    @Test
    void visionRuntimeConfigReusesQwenTextApiKeyWhenVisionKeyIsMissing() {
        AiModelConfigMapper mapper = mock(AiModelConfigMapper.class);
        when(mapper.selectOne(any())).thenReturn(
                config("qwen", "qwen-vl-plus", "vision", null),
                config("qwen", "qwen-max", "text_recipe", "qwen-dashboard-key")
        );
        AiModelConfigService service = new AiModelConfigService(mapper, qwenProperties(""));

        AiModelRuntimeConfig runtimeConfig = service.visionRuntimeConfig();

        assertThat(runtimeConfig.provider()).isEqualTo("qwen");
        assertThat(runtimeConfig.modelName()).isEqualTo("qwen-vl-plus");
        assertThat(runtimeConfig.apiKey()).isEqualTo("qwen-dashboard-key");
    }

    @Test
    void visionRuntimeConfigDoesNotReuseDeepseekTextApiKey() {
        AiModelConfigMapper mapper = mock(AiModelConfigMapper.class);
        when(mapper.selectOne(any())).thenReturn(
                config("qwen", "qwen-vl-plus", "vision", null),
                config("deepseek", "deepseek-v4-flash", "text_recipe", "deepseek-dashboard-key")
        );
        AiModelConfigService service = new AiModelConfigService(mapper, qwenProperties(""));

        AiModelRuntimeConfig runtimeConfig = service.visionRuntimeConfig();

        assertThat(runtimeConfig.provider()).isEqualTo("qwen");
        assertThat(runtimeConfig.modelName()).isEqualTo("qwen-vl-plus");
        assertThat(runtimeConfig.apiKey()).isBlank();
    }

    private QwenProperties qwenProperties(String apiKey) {
        QwenProperties properties = new QwenProperties(apiKey, "qwen-plus", "https://dashscope.test/chat/completions");
        properties.setVisionModel("qwen-vl-plus");
        return properties;
    }

    private AiModelConfig config(String provider, String modelName, String purpose, String apiKey) {
        AiModelConfig config = new AiModelConfig();
        config.setProvider(provider);
        config.setModelName(modelName);
        config.setPurpose(purpose);
        config.setPrimaryModel(true);
        config.setEnabled(true);
        config.setApiKey(apiKey);
        return config;
    }
}
