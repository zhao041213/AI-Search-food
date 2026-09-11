package com.example.food.ai.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.food.ai.config.dto.AiModelConfigResponse;
import com.example.food.ai.config.dto.AiModelConfigUpdateRequest;
import com.example.food.ai.qwen.QwenProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AiModelConfigService {

    private static final String TEXT_RECIPE_PURPOSE = "text_recipe";
    private static final String VISION_PURPOSE = "vision";
    private static final String DEFAULT_PROVIDER = "qwen";
    private static final String OPENAI_PROTOCOL = "openai";
    private static final String ANTHROPIC_PROTOCOL = "anthropic";
    private static final String DEFAULT_QWEN_OPENAI_ENDPOINT =
            "https://dashscope.aliyuncs.com/compatible-mode/v1";
    private static final String DEFAULT_QWEN_ANTHROPIC_ENDPOINT =
            "https://dashscope.aliyuncs.com/apps/anthropic";
    private static final String DEFAULT_DEEPSEEK_ENDPOINT = "https://api.deepseek.com/v1";

    private final AiModelConfigMapper aiModelConfigMapper;
    private final QwenProperties qwenProperties;
    private final DotEnvSynchronizer dotEnvSynchronizer;

    @Autowired
    public AiModelConfigService(
            AiModelConfigMapper aiModelConfigMapper,
            QwenProperties qwenProperties,
            DotEnvSynchronizer dotEnvSynchronizer
    ) {
        this.aiModelConfigMapper = aiModelConfigMapper;
        this.qwenProperties = qwenProperties;
        this.dotEnvSynchronizer = dotEnvSynchronizer;
    }

    public AiModelConfigService(AiModelConfigMapper aiModelConfigMapper, QwenProperties qwenProperties) {
        this(aiModelConfigMapper, qwenProperties, new DotEnvSynchronizer(java.nio.file.Path.of("../.env")));
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
                valueOrDefault(config.getApiProtocol(), OPENAI_PROTOCOL),
                valueOrDefault(config.getModelName(), qwenProperties.model()),
                valueOrDefault(config.getEndpointUrl(), qwenProperties.endpoint()),
                valueOrDefault(config.getApiKey(), qwenProperties.apiKey())
        );
    }

    public AiModelRuntimeConfig visionRuntimeConfig() {
        AiModelConfig config = primaryConfig(VISION_PURPOSE);
        AiModelRuntimeConfig textRuntimeConfig = textRecipeRuntimeConfig();
        String fallbackApiKey = qwenFallbackApiKey(textRuntimeConfig);
        if (config == null || !Boolean.TRUE.equals(config.getEnabled())) {
            return new AiModelRuntimeConfig(
                    DEFAULT_PROVIDER,
                    DEFAULT_PROVIDER.equalsIgnoreCase(textRuntimeConfig.provider())
                            ? textRuntimeConfig.protocol()
                            : OPENAI_PROTOCOL,
                    qwenProperties.visionModel(),
                    DEFAULT_PROVIDER.equalsIgnoreCase(textRuntimeConfig.provider())
                            ? textRuntimeConfig.endpoint()
                            : qwenProperties.endpoint(),
                    fallbackApiKey
            );
        }
        boolean useTextEndpoint = !hasText(config.getEndpointUrl())
                && DEFAULT_PROVIDER.equalsIgnoreCase(config.getProvider())
                && DEFAULT_PROVIDER.equalsIgnoreCase(textRuntimeConfig.provider());
        return new AiModelRuntimeConfig(
                valueOrDefault(config.getProvider(), DEFAULT_PROVIDER),
                useTextEndpoint
                        ? textRuntimeConfig.protocol()
                        : valueOrDefault(config.getApiProtocol(), OPENAI_PROTOCOL),
                valueOrDefault(config.getModelName(), qwenProperties.visionModel()),
                useTextEndpoint
                        ? textRuntimeConfig.endpoint()
                        : valueOrDefault(config.getEndpointUrl(), qwenProperties.endpoint()),
                valueOrDefault(
                        config.getApiKey(),
                        DEFAULT_PROVIDER.equalsIgnoreCase(config.getProvider())
                                ? fallbackApiKey
                                : qwenProperties.apiKey()
                )
        );
    }

    @Transactional
    public AiModelConfigResponse saveTextRecipeConfig(AiModelConfigUpdateRequest request) {
        String provider = normalize(request.provider()).toLowerCase();
        String protocol = normalizeProtocol(request.protocol());
        String modelName = normalize(request.modelName());
        String endpoint = resolveEndpoint(provider, protocol, request.endpoint());
        boolean enabled = request.enabled() == null || request.enabled();

        AiModelConfig currentPrimary = primaryTextRecipeConfig();
        AiModelConfig target = selectByProviderModelPurpose(provider, modelName, TEXT_RECIPE_PURPOSE);
        if (target == null) {
            target = currentPrimary != null
                    && provider.equalsIgnoreCase(currentPrimary.getProvider())
                    ? currentPrimary
                    : new AiModelConfig();
        } else if (currentPrimary != null && !target.getId().equals(currentPrimary.getId())) {
            currentPrimary.setPrimaryModel(false);
            currentPrimary.setUpdatedAt(LocalDateTime.now());
            aiModelConfigMapper.updateById(currentPrimary);
        }

        target.setProvider(provider);
        target.setApiProtocol(protocol);
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
        dotEnvSynchronizer.synchronize(target);
        return responseFor(target);
    }

    public AiModelRuntimeConfig previewRuntimeConfig(AiModelConfigUpdateRequest request) {
        String provider = normalize(request.provider()).toLowerCase();
        String protocol = normalizeProtocol(request.protocol());
        String modelName = normalize(request.modelName());
        String endpoint = resolveEndpoint(provider, protocol, request.endpoint());
        String apiKey = hasText(request.apiKey())
                ? request.apiKey().trim()
                : apiKeyFor(provider, modelName);
        return new AiModelRuntimeConfig(provider, protocol, modelName, endpoint, apiKey);
    }

    private AiModelConfig primaryTextRecipeConfig() {
        return primaryConfig(TEXT_RECIPE_PURPOSE);
    }

    private AiModelConfig primaryConfig(String purpose) {
        return aiModelConfigMapper.selectOne(new QueryWrapper<AiModelConfig>()
                .eq("purpose", purpose)
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
                    OPENAI_PROTOCOL,
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
                valueOrDefault(config.getApiProtocol(), OPENAI_PROTOCOL),
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
                OPENAI_PROTOCOL,
                qwenProperties.model(),
                qwenProperties.endpoint(),
                qwenProperties.apiKey()
        );
    }

    private String apiKeyFor(String provider, String modelName) {
        AiModelConfig selected = selectByProviderModelPurpose(provider, modelName, TEXT_RECIPE_PURPOSE);
        if (selected != null && hasText(selected.getApiKey())) {
            return selected.getApiKey().trim();
        }
        AiModelConfig current = primaryTextRecipeConfig();
        if (current != null
                && provider.equalsIgnoreCase(current.getProvider())
                && hasText(current.getApiKey())) {
            return current.getApiKey().trim();
        }
        return DEFAULT_PROVIDER.equalsIgnoreCase(provider) ? qwenProperties.apiKey() : "";
    }

    private String resolveEndpoint(String provider, String protocol, String endpoint) {
        return hasText(endpoint) ? endpoint.trim() : defaultEndpoint(provider, protocol);
    }

    private String defaultEndpoint(String provider, String protocol) {
        if (DEFAULT_PROVIDER.equalsIgnoreCase(provider)) {
            if (ANTHROPIC_PROTOCOL.equals(protocol)) {
                return DEFAULT_QWEN_ANTHROPIC_ENDPOINT;
            }
            return baseEndpoint(qwenProperties.endpoint(), DEFAULT_QWEN_OPENAI_ENDPOINT);
        }
        if (ANTHROPIC_PROTOCOL.equals(protocol)) {
            throw new IllegalArgumentException("当前服务商暂不支持 Anthropic 兼容协议");
        }
        return DEFAULT_DEEPSEEK_ENDPOINT;
    }

    private String baseEndpoint(String endpoint, String fallback) {
        if (!hasText(endpoint)) {
            return fallback;
        }
        String normalized = endpoint.trim();
        String suffix = "/chat/completions";
        if (normalized.endsWith(suffix)) {
            return normalized.substring(0, normalized.length() - suffix.length());
        }
        return normalized;
    }

    private String normalizeProtocol(String protocol) {
        if (!hasText(protocol)) {
            return OPENAI_PROTOCOL;
        }
        String normalized = protocol.trim().toLowerCase();
        if (!OPENAI_PROTOCOL.equals(normalized) && !ANTHROPIC_PROTOCOL.equals(normalized)) {
            throw new IllegalArgumentException("接口协议仅支持 OpenAI 兼容或 Anthropic 兼容");
        }
        return normalized;
    }

    private String qwenFallbackApiKey(AiModelRuntimeConfig textRuntimeConfig) {
        if (DEFAULT_PROVIDER.equalsIgnoreCase(textRuntimeConfig.provider())) {
            return valueOrDefault(textRuntimeConfig.apiKey(), qwenProperties.apiKey());
        }
        return qwenProperties.apiKey();
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
