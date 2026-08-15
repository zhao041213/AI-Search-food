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
import java.util.ArrayList;
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

    public FinishedDishReview reviewFinishedDish(
            String contentType,
            byte[] imageBytes,
            FinishedDishReviewContext context
    ) {
        AiModelRuntimeConfig runtimeConfig = runtimeConfig();
        if (runtimeConfig.apiKey() == null || runtimeConfig.apiKey().isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "千问 API Key 未配置，请设置 DASHSCOPE_API_KEY");
        }

        try {
            ResponseEntity<QwenChatResponse> response = restTemplate.exchange(
                    runtimeConfig.endpoint(),
                    HttpMethod.POST,
                    new HttpEntity<>(requestBody(
                            contentType,
                            imageBytes,
                            runtimeConfig,
                            finishedDishPrompt(context),
                            "你是一名专业的中文烹饪视觉评估助手。你只根据照片可见信息给出中性、可执行的烹饪建议，不作食品安全、医疗或健康诊断。"
                    ), headers(runtimeConfig)),
                    QwenChatResponse.class
            );
            return parseFinishedDishReview(firstContent(response.getBody()), runtimeConfig);
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "千问成品图评价服务调用失败，请稍后重试", exception);
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
        return requestBody(
                contentType,
                imageBytes,
                runtimeConfig,
                prompt,
                "你是一个专业中文食材识别助手，输出必须是可解析 JSON。"
        );
    }

    private Map<String, Object> requestBody(
            String contentType,
            byte[] imageBytes,
            AiModelRuntimeConfig runtimeConfig,
            String prompt,
            String systemPrompt
    ) {
        return Map.of(
                "model", runtimeConfig.modelName(),
                "messages", List.of(
                        Map.of(
                                "role", "system",
                                "content", systemPrompt
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

    private String finishedDishPrompt(FinishedDishReviewContext context) {
        String recipeTitle = context == null ? "未命名菜谱" : textOrDefault(context.recipeTitle(), "未命名菜谱");
        String ingredients = context == null ? "未提供" : joinContextItems(context.ingredients());
        String steps = context == null ? "未提供" : joinContextItems(context.steps());
        return """
                请结合成品照片和菜谱上下文，给出一次成品图视觉烹饪评价。
                菜谱标题：%s
                主要食材：%s
                烹饪步骤：%s

                评价只描述照片中可见的色泽、火候、摆盘和成品完整度。不要断言食物是否绝对安全、是否变质，也不要给出医疗建议。
                只返回 JSON，不要 Markdown，不要额外说明。JSON 格式必须为：
                {"overallScore":0,"summary":"一句中文总评","color":{"score":0,"comment":"中文说明"},"doneness":{"score":0,"comment":"中文说明"},"plating":{"score":0,"comment":"中文说明"},"strengths":["优点"],"issues":[{"severity":"low","title":"问题标题","evidence":"照片依据","suggestion":"下次改进做法"}]}
                所有 score 都是 0 到 100 的整数；severity 仅使用 low、medium 或 high。
                """.formatted(recipeTitle, ingredients, steps);
    }

    private String joinContextItems(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "未提供";
        }
        List<String> normalized = values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .limit(20)
                .toList();
        return normalized.isEmpty() ? "未提供" : String.join("；", normalized);
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

    private FinishedDishReview parseFinishedDishReview(
            String content,
            AiModelRuntimeConfig runtimeConfig
    ) {
        try {
            FinishedDishReviewPayload payload = objectMapper.readValue(
                    stripJsonFence(content),
                    FinishedDishReviewPayload.class
            );
            return new FinishedDishReview(
                    score(payload.overallScore(), "总体评分"),
                    dimension(payload.color(), "色泽"),
                    dimension(payload.doneness(), "火候"),
                    dimension(payload.plating(), "摆盘"),
                    requiredText(payload.summary(), "总体评价"),
                    textList(payload.strengths()),
                    issues(payload.issues()),
                    "仅根据照片提供视觉烹饪建议，无法替代食品安全判断。",
                    runtimeConfig.provider(),
                    runtimeConfig.modelName()
            );
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "千问成品图评价返回内容不是有效 JSON", exception);
        }
    }

    private VisualDimension dimension(DimensionPayload payload, String label) {
        if (payload == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "千问成品图评价缺少" + label + "字段");
        }
        return new VisualDimension(score(payload.score(), label + "评分"), requiredText(payload.comment(), label + "说明"));
    }

    private int score(Integer value, String label) {
        if (value == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "千问成品图评价缺少" + label);
        }
        return Math.max(0, Math.min(100, value));
    }

    private String requiredText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "千问成品图评价缺少" + label);
        }
        String normalized = value.trim();
        return normalized.length() <= 500 ? normalized : normalized.substring(0, 500);
    }

    private String textOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private List<String> textList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.trim().length() <= 180 ? value.trim() : value.trim().substring(0, 180))
                .limit(6)
                .toList();
    }

    private List<FinishedDishIssue> issues(List<IssuePayload> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<FinishedDishIssue> result = new ArrayList<>();
        for (IssuePayload value : values) {
            if (value == null || value.title() == null || value.title().isBlank()) {
                continue;
            }
            result.add(new FinishedDishIssue(
                    severity(value.severity()),
                    requiredText(value.title(), "问题标题"),
                    textOrDefault(value.evidence(), "照片中未提供明确依据"),
                    requiredText(value.suggestion(), "改进建议")
            ));
            if (result.size() == 6) {
                break;
            }
        }
        return List.copyOf(result);
    }

    private String severity(String value) {
        if (value == null) {
            return "medium";
        }
        String normalized = value.trim().toLowerCase();
        if ("high".equals(normalized) || "高".equals(normalized)) {
            return "high";
        }
        if ("low".equals(normalized) || "低".equals(normalized)) {
            return "low";
        }
        return "medium";
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record FinishedDishReviewPayload(
            Integer overallScore,
            String summary,
            DimensionPayload color,
            DimensionPayload doneness,
            DimensionPayload plating,
            List<String> strengths,
            List<IssuePayload> issues
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record DimensionPayload(Integer score, String comment) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record IssuePayload(String severity, String title, String evidence, String suggestion) {
    }

    public record IngredientImageVerification(boolean matches, double confidence, String description) {
    }

    public record FinishedDishReviewContext(String recipeTitle, List<String> ingredients, List<String> steps) {
    }

    public record FinishedDishReview(
            int overallScore,
            VisualDimension color,
            VisualDimension doneness,
            VisualDimension plating,
            String summary,
            List<String> strengths,
            List<FinishedDishIssue> issues,
            String safetyNote,
            String provider,
            String model
    ) {
    }

    public record VisualDimension(int score, String comment) {
    }

    public record FinishedDishIssue(String severity, String title, String evidence, String suggestion) {
    }
}
