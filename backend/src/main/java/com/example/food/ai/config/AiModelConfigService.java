package com.example.food.ai.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.food.ai.config.dto.AiModelConfigResponse;
import com.example.food.ai.config.dto.AiModelConfigUpdateRequest;
import com.example.food.ai.qwen.QwenProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AiModelConfigService {

    private static final String TEXT_RECIPE_PURPOSE = "text_recipe";
    private static final String DEFAULT_PROVIDER = "qwen";

    private final AiModelConfigMapper aiModelConfigMapper;
    private final QwenProperties qwenProperties;

    public AiModelConfigService(AiModelConfigMapper aiModelConfigMapper, QwenProperties qwenProperties) {
        this.aiModelConfigMapper = aiModelConfigMapper;
        this.qwenProperties = qwenProperties;
    }

    public AiModelConfigResponse textRecipeConfig() {
        return responseFor(primaryTextRecipeConfig());
    }

    public AiModelRuntimeConfig textRecipeRuntimeConfig() {
        AiModelConfig config = primaryTextRecipeConfig();
        if (config == null || !Boolean.TRUE.equals(config.getEnabled())) {
            return fallbackRuntimeConfig();
        }
        return new AiModelRuntimeConfig(
                valueOrDefault(config.getProvider(), DEFAULT_PROVIDER),
                valueOrDefault(config.getModelName(), qwenProperties.model()),
                valueOrDefault(config.getEndpointUrl(), qwenProperties.endpoint()),
                valueOrDefault(config.getApiKey(), qwenProperties.apiKey())
        );
    }

    @Transactional
    public AiModelConfigResponse saveTextRecipeConfig(AiModelConfigUpdateRequest request) {
        String provider = normalize(request.provider()).toLowerCase();
        String modelName = normalize(request.modelName());
        String endpoint = normalize(request.endpoint());
        boolean enabled = request.enabled() == null || request.enabled();

        AiModelConfig currentPrimary = primaryTextRecipeConfig();
        AiModelConfig target = selectByProviderModelPurpose(provider, modelName, TEXT_RECIPE_PURPOSE);
        if (target == null) {
            target = currentPrimary == null ? new AiModelConfig() : currentPrimary;
        } else if (currentPrimary != null && !target.getId().equals(currentPrimary.getId())) {
            currentPrimary.setPrimaryModel(false);
            currentPrimary.setUpdatedAt(LocalDateTime.now());
            aiModelConfigMapper.updateById(currentPrimary);
        }

        target.setProvider(provider);
        target.setModelName(modelName);
        target.setPurpose(TEXT_RECIPE_PURPOSE);
        target.setEndpointUrl(endpoint);
        target.setEnabled(enabled);
        target.setPrimaryModel(true);
        if (hasText(request.apiKey())) {
            target.setApiKey(request.apiKey().trim());
        }
        target.setUpdatedAt(LocalDateTime.now());

        if (target.getId() == null) {
            target.setCreatedAt(LocalDateTime.now());
            aiModelConfigMapper.insert(target);
        } else {
            aiModelConfigMapper.updateById(target);
        }
        return responseFor(target);
    }

    private AiModelConfig primaryTextRecipeConfig() {
        return aiModelConfigMapper.selectOne(new QueryWrapper<AiModelConfig>()
                .eq("purpose", TEXT_RECIPE_PURPOSE)
                .eq("primary_model", 1)
                .last("LIMIT 1"));
    }

    private AiModelConfig selectByProviderModelPurpose(String provider, String modelName, String purpose) {
        return aiModelConfigMapper.selectOne(new QueryWrapper<AiModelConfig>()
                .eq("provider", provider)
                .eq("model_name", modelName)
                .eq("purpose", purpose)
                .last("LIMIT 1"));
    }

    private AiModelConfigResponse responseFor(AiModelConfig config) {
        if (config == null) {
            return new AiModelConfigResponse(
                    DEFAULT_PROVIDER,
                    qwenProperties.model(),
                    TEXT_RECIPE_PURPOSE,
                    qwenProperties.endpoint(),
                    true,
                    true,
                    false,
                    ""
            );
        }
        return new AiModelConfigResponse(
                valueOrDefault(config.getProvider(), DEFAULT_PROVIDER),
                valueOrDefault(config.getModelName(), qwenProperties.model()),
                TEXT_RECIPE_PURPOSE,
                valueOrDefault(config.getEndpointUrl(), qwenProperties.endpoint()),
                Boolean.TRUE.equals(config.getEnabled()),
                Boolean.TRUE.equals(config.getPrimaryModel()),
                hasText(config.getApiKey()),
                apiKeyPreview(config.getApiKey())
        );
    }

    private AiModelRuntimeConfig fallbackRuntimeConfig() {
        return new AiModelRuntimeConfig(
                DEFAULT_PROVIDER,
                qwenProperties.model(),
                qwenProperties.endpoint(),
                qwenProperties.apiKey()
        );
    }

    private String apiKeyPreview(String apiKey) {
        if (!hasText(apiKey)) {
            return "";
        }
        String trimmed = apiKey.trim();
        String suffix = trimmed.length() <= 4 ? trimmed : trimmed.substring(trimmed.length() - 4);
        return "****" + suffix;
    }

    private String valueOrDefault(String value, String defaultValue) {
        return hasText(value) ? value.trim() : defaultValue;
    }

    private String normalize(String value) {
        if (!hasText(value)) {
            throw new IllegalArgumentException("AI 模型配置参数不能为空");
        }
        return value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
