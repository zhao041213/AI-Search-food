package com.example.food.ai.qwen;

import com.example.food.ai.config.AiModelConfigService;
import com.example.food.ai.config.AiModelRuntimeConfig;
import com.example.food.ai.recipe.dto.RecipeGenerateResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class QwenRecipeClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final QwenProperties properties;
    private final AiModelConfigService aiModelConfigService;

    @Autowired
    public QwenRecipeClient(
            RestTemplateBuilder restTemplateBuilder,
            ObjectMapper objectMapper,
            QwenProperties properties,
            AiModelConfigService aiModelConfigService
    ) {
        this(
                restTemplateBuilder
                        .setConnectTimeout(Duration.ofSeconds(5))
                        .setReadTimeout(Duration.ofSeconds(60))
                        .build(),
                objectMapper,
                properties,
                aiModelConfigService
        );
    }

    public QwenRecipeClient(RestTemplate restTemplate, ObjectMapper objectMapper, QwenProperties properties) {
        this(restTemplate, objectMapper, properties, null);
    }

    public QwenRecipeClient(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            QwenProperties properties,
            AiModelConfigService aiModelConfigService
    ) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.aiModelConfigService = aiModelConfigService;
    }

    public RecipeGenerateResponse generateRecipe(String prompt) {
        AiModelRuntimeConfig runtimeConfig = runtimeConfig();
        if (runtimeConfig.apiKey() == null || runtimeConfig.apiKey().isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "千问 API Key 未配置，请设置 DASHSCOPE_API_KEY");
        }

        try {
            ResponseEntity<QwenChatResponse> response = restTemplate.exchange(
                    runtimeConfig.endpoint(),
                    HttpMethod.POST,
                    new HttpEntity<>(requestBody(prompt, runtimeConfig), headers(runtimeConfig)),
                    QwenChatResponse.class
            );
            return parseRecipe(response.getBody(), runtimeConfig);
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "千问服务调用失败，请稍后重试", exception);
        }
    }

    private AiModelRuntimeConfig runtimeConfig() {
        if (aiModelConfigService != null) {
            return aiModelConfigService.textRecipeRuntimeConfig();
        }
        return new AiModelRuntimeConfig("qwen", properties.model(), properties.endpoint(), properties.apiKey());
    }

    private Map<String, Object> requestBody(String prompt, AiModelRuntimeConfig runtimeConfig) {
        return Map.of(
                "model", runtimeConfig.modelName(),
                "messages", List.of(
                        Map.of(
                                "role", "system",
                                "content", "你是一个专业中文营养师和家庭烹饪助手，输出必须是可解析 JSON。"
                        ),
                        Map.of(
                                "role", "user",
                                "content", prompt
                        )
                ),
                "temperature", 0.7
        );
    }

    private HttpHeaders headers(AiModelRuntimeConfig runtimeConfig) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(runtimeConfig.apiKey());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private RecipeGenerateResponse parseRecipe(QwenChatResponse response, AiModelRuntimeConfig runtimeConfig) {
        String content = firstContent(response);
        RecipePayload payload = readPayload(content);
        return new RecipeGenerateResponse(
                payload.title(),
                payload.summary(),
                payload.effects(),
                payload.ingredients(),
                payload.steps(),
                payload.tips(),
                payload.videoKeywords(),
                runtimeConfig.provider(),
                runtimeConfig.modelName()
        );
    }

    private String firstContent(QwenChatResponse response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "千问服务未返回菜谱内容");
        }
        QwenChoice choice = response.choices().get(0);
        if (choice.message() == null || choice.message().content() == null || choice.message().content().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "千问服务未返回菜谱内容");
        }
        return choice.message().content();
    }

    private RecipePayload readPayload(String content) {
        try {
            return objectMapper.readValue(stripJsonFence(content), RecipePayload.class);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "千问返回内容不是有效菜谱 JSON", exception);
        }
    }

    private String stripJsonFence(String content) {
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(?:json)?\\s*", "");
            trimmed = trimmed.replaceFirst("\\s*```$", "");
        }
        return trimmed.trim();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record QwenChatResponse(List<QwenChoice> choices) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record QwenChoice(QwenMessage message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record QwenMessage(String content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RecipePayload(
            String title,
            String summary,
            List<String> effects,
            List<RecipeGenerateResponse.Ingredient> ingredients,
            List<RecipeGenerateResponse.Step> steps,
            List<String> tips,
            List<String> videoKeywords
    ) {
    }
}
