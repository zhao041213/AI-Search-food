package com.example.food.ai.qwen;

import com.example.food.ai.config.AiModelConfigService;
import com.example.food.ai.config.AiModelRuntimeConfig;
import com.example.food.ai.ingredient.dto.IngredientRecognitionResponse;
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
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
public class QwenVisionClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final QwenProperties properties;
    private final AiModelConfigService aiModelConfigService;

    @Autowired
    public QwenVisionClient(
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

    public QwenVisionClient(RestTemplate restTemplate, ObjectMapper objectMapper, QwenProperties properties) {
        this(restTemplate, objectMapper, properties, null);
    }

    public QwenVisionClient(
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

    public IngredientRecognitionResponse recognizeIngredients(String contentType, byte[] imageBytes) {
        AiModelRuntimeConfig runtimeConfig = runtimeConfig();
        if (runtimeConfig.apiKey() == null || runtimeConfig.apiKey().isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "千问 API Key 未配置，请设置 DASHSCOPE_API_KEY");
        }

        try {
            ResponseEntity<QwenChatResponse> response = restTemplate.exchange(
                    runtimeConfig.endpoint(),
                    HttpMethod.POST,
                    new HttpEntity<>(requestBody(contentType, imageBytes, runtimeConfig, recognitionPrompt()), headers(runtimeConfig)),
                    QwenChatResponse.class
            );
            return parseRecognition(response.getBody(), runtimeConfig);
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "千问视觉服务调用失败，请稍后重试", exception);
        }
    }

    public IngredientImageVerification verifyIngredientImage(
            String contentType,
            byte[] imageBytes,
            String canonicalName
    ) {
        AiModelRuntimeConfig runtimeConfig = runtimeConfig();
        if (runtimeConfig.apiKey() == null || runtimeConfig.apiKey().isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "千问 API Key 未配置");
        }

        try {
            ResponseEntity<QwenChatResponse> response = restTemplate.exchange(
                    runtimeConfig.endpoint(),
                    HttpMethod.POST,
                    new HttpEntity<>(requestBody(
                            contentType,
                            imageBytes,
                            runtimeConfig,
                            verificationPrompt(canonicalName)
                    ), headers(runtimeConfig)),
                    QwenChatResponse.class
            );
            return parseVerification(firstContent(response.getBody()));
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "千问视觉校验调用失败", exception);
        }
    }

    private AiModelRuntimeConfig runtimeConfig() {
        if (aiModelConfigService != null) {
            return aiModelConfigService.visionRuntimeConfig();
        }
        return new AiModelRuntimeConfig("qwen", properties.visionModel(), properties.endpoint(), properties.apiKey());
    }

    private Map<String, Object> requestBody(
            String contentType,
            byte[] imageBytes,
            AiModelRuntimeConfig runtimeConfig,
            String prompt
    ) {
        return Map.of(
                "model", runtimeConfig.modelName(),
                "messages", List.of(
                        Map.of(
                                "role", "system",
                                "content", "你是一个专业中文食材识别助手，输出必须是可解析 JSON。"
                        ),
                        Map.of(
                                "role", "user",
                                "content", List.of(
                                        Map.of(
                                                "type", "text",
                                                "text", prompt
                                        ),
                                        Map.of(
                                                "type", "image_url",
                                                "image_url", Map.of("url", dataUrl(contentType, imageBytes))
                                        )
                                )
                        )
                ),
                "temperature", 0.1
        );
    }

    private String recognitionPrompt() {
        return "请识别图片中清晰可见的可食用食材，只返回 JSON："
                + "{\"ingredients\":[\"食材1\",\"食材2\"],\"description\":\"一句中文描述\"}"
                + "。如果无法识别，ingredients 返回空数组。";
    }

    private String verificationPrompt(String canonicalName) {
        return "请判断图片中是否清晰展示了名为“" + canonicalName + "”的可食用食材。"
                + "只返回 JSON：{\"matches\":true,\"confidence\":0.0,\"description\":\"一句中文说明\"}。"
                + "matches 只有在主体确实是该食材时才为 true，confidence 为 0 到 1 的数字。";
    }

    private String dataUrl(String contentType, byte[] imageBytes) {
        return "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(imageBytes);
    }

    private HttpHeaders headers(AiModelRuntimeConfig runtimeConfig) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(runtimeConfig.apiKey());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private IngredientRecognitionResponse parseRecognition(QwenChatResponse response, AiModelRuntimeConfig runtimeConfig) {
        IngredientPayload payload = readPayload(firstContent(response));
        return new IngredientRecognitionResponse(
                payload.ingredients(),
                payload.description(),
                runtimeConfig.provider(),
                runtimeConfig.modelName()
        );
    }

    private String firstContent(QwenChatResponse response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "千问视觉服务未返回识别内容");
        }
        QwenChoice choice = response.choices().get(0);
        if (choice.message() == null || choice.message().content() == null || choice.message().content().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "千问视觉服务未返回识别内容");
        }
        return choice.message().content();
    }

    private IngredientPayload readPayload(String content) {
        try {
            return objectMapper.readValue(stripJsonFence(content), IngredientPayload.class);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "千问视觉返回内容不是有效食材 JSON", exception);
        }
    }

    private IngredientImageVerification parseVerification(String content) {
        try {
            VerificationPayload payload = objectMapper.readValue(
                    stripJsonFence(content),
                    VerificationPayload.class
            );
            return new IngredientImageVerification(
                    Boolean.TRUE.equals(payload.matches()),
                    payload.confidence() == null ? 0d : payload.confidence(),
                    payload.description()
            );
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "千问视觉校验返回内容不是有效 JSON", exception);
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
    private record IngredientPayload(List<String> ingredients, String description) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record VerificationPayload(Boolean matches, Double confidence, String description) {
    }

    public record IngredientImageVerification(boolean matches, double confidence, String description) {
    }
}
