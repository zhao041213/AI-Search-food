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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

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
                        .setReadTimeout(Duration.ofSeconds(120))
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

    public RecipeStreamResult streamRecipe(
            String prompt,
            Consumer<String> onDelta,
            Runnable onFirstChunk
    ) {
        AiModelRuntimeConfig runtimeConfig = runtimeConfig();
        if (runtimeConfig.apiKey() == null || runtimeConfig.apiKey().isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "千问 API Key 未配置，请设置 DASHSCOPE_API_KEY");
        }

        try {
            return restTemplate.execute(
                    runtimeConfig.endpoint(),
                    HttpMethod.POST,
                    request -> {
                        request.getHeaders().putAll(headers(runtimeConfig, true));
                        objectMapper.writeValue(request.getBody(), requestBody(prompt, runtimeConfig, true));
                    },
                    response -> readStreamResponse(response, runtimeConfig, onDelta, onFirstChunk)
            );
        } catch (org.springframework.web.client.RestClientResponseException exception) {
            if (isNativeStreamingUnsupported(exception.getStatusCode().value())) {
                return new RecipeStreamResult(
                        "",
                        generateRecipe(prompt),
                        runtimeConfig.provider(),
                        runtimeConfig.modelName(),
                        false
                );
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "千问流式服务调用失败，请稍后重试", exception);
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "千问流式服务调用失败，请稍后重试", exception);
        }
    }

    public RecipeGenerateResponse parseRecipeContent(String content, AiModelRuntimeConfig runtimeConfig) {
        return parseRecipePayload(content, runtimeConfig);
    }

    public List<WeeklyMenuSelection> generateWeeklyMenu(String prompt) {
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
            return parseWeeklyMenu(response.getBody());
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

    public AiModelRuntimeConfig currentRuntimeConfig() {
        return runtimeConfig();
    }

    private Map<String, Object> requestBody(String prompt, AiModelRuntimeConfig runtimeConfig) {
        return requestBody(prompt, runtimeConfig, false);
    }

    private Map<String, Object> requestBody(
            String prompt,
            AiModelRuntimeConfig runtimeConfig,
            boolean stream
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", runtimeConfig.modelName());
        body.put("messages", List.of(
                Map.of(
                        "role", "system",
                        "content", "你是专业的中文营养与家庭烹饪助手。只输出可解析的 JSON，不要输出 Markdown、代码块或额外说明。"
                ),
                Map.of(
                        "role", "user",
                        "content", prompt
                )
        ));
        body.put("temperature", 0.7);
        if (stream) {
            body.put("stream", true);
        }
        return body;
    }

    private HttpHeaders headers(AiModelRuntimeConfig runtimeConfig) {
        return headers(runtimeConfig, false);
    }

    private HttpHeaders headers(AiModelRuntimeConfig runtimeConfig, boolean stream) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(runtimeConfig.apiKey());
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (stream) {
            headers.setAccept(List.of(MediaType.TEXT_EVENT_STREAM, MediaType.APPLICATION_JSON));
        }
        return headers;
    }

    private RecipeGenerateResponse parseRecipe(QwenChatResponse response, AiModelRuntimeConfig runtimeConfig) {
        String content = firstContent(response);
        return parseRecipePayload(content, runtimeConfig);
    }

    private RecipeGenerateResponse parseRecipePayload(String content, AiModelRuntimeConfig runtimeConfig) {
        RecipePayload payload = readPayload(content);
        return new RecipeGenerateResponse(
                payload.title(),
                payload.summary(),
                payload.effects(),
                payload.ingredients(),
                payload.missingIngredients(),
                payload.steps(),
                payload.tips(),
            payload.videoKeywords(),
            payload.explanation(),
                RecipeGenerateResponse.NutritionEstimate.from(payload.nutritionEstimate()),
                runtimeConfig.provider(),
                runtimeConfig.modelName()
        );
    }

    private RecipeStreamResult readStreamResponse(
            org.springframework.http.client.ClientHttpResponse response,
            AiModelRuntimeConfig runtimeConfig,
            Consumer<String> onDelta,
            Runnable onFirstChunk
    ) throws IOException {
        MediaType contentType = response.getHeaders().getContentType();
        boolean sse = contentType != null && contentType.isCompatibleWith(MediaType.TEXT_EVENT_STREAM);
        if (!sse) {
            byte[] body = response.getBody().readAllBytes();
            String text = new String(body, StandardCharsets.UTF_8).trim();
            sse = text.startsWith("data:") || text.startsWith(":");
            if (!sse) {
                QwenChatResponse chatResponse = objectMapper.readValue(body, QwenChatResponse.class);
                return new RecipeStreamResult(
                        "",
                        parseRecipe(chatResponse, runtimeConfig),
                        runtimeConfig.provider(),
                        runtimeConfig.modelName(),
                        false
                );
            }
            return readSse(text, runtimeConfig, onDelta, onFirstChunk);
        }
        return readSse(
                new BufferedReader(new InputStreamReader(response.getBody(), StandardCharsets.UTF_8)),
                runtimeConfig,
                onDelta,
                onFirstChunk
        );
    }

    private RecipeStreamResult readSse(
            String text,
            AiModelRuntimeConfig runtimeConfig,
            Consumer<String> onDelta,
            Runnable onFirstChunk
    ) throws IOException {
        return readSse(
                new BufferedReader(new java.io.StringReader(text)),
                runtimeConfig,
                onDelta,
                onFirstChunk
        );
    }

    private RecipeStreamResult readSse(
            BufferedReader reader,
            AiModelRuntimeConfig runtimeConfig,
            Consumer<String> onDelta,
            Runnable onFirstChunk
    ) throws IOException {
        StringBuilder content = new StringBuilder();
        List<String> dataLines = new ArrayList<>();
        boolean done = false;
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isEmpty()) {
                done = processSseEvent(dataLines, content, onDelta, onFirstChunk) || done;
                dataLines.clear();
                continue;
            }
            if (line.startsWith(":")) {
                continue;
            }
            if (line.startsWith("data:")) {
                String data = line.substring(5);
                dataLines.add(data.startsWith(" ") ? data.substring(1) : data);
            }
        }
        done = processSseEvent(dataLines, content, onDelta, onFirstChunk) || done;
        if (!done) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "千问流式响应中断，请点击重试");
        }
        return new RecipeStreamResult(
                content.toString(),
                null,
                runtimeConfig.provider(),
                runtimeConfig.modelName(),
                true
        );
    }

    private boolean processSseEvent(
            List<String> dataLines,
            StringBuilder content,
            Consumer<String> onDelta,
            Runnable onFirstChunk
    ) throws IOException {
        if (dataLines.isEmpty()) {
            return false;
        }
        String data = String.join("\n", dataLines).trim();
        if ("[DONE]".equals(data)) {
            return true;
        }
        try {
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(data);
            String delta = streamContent(root);
            if (delta != null && !delta.isEmpty()) {
                if (content.isEmpty()) {
                    onFirstChunk.run();
                }
                content.append(delta);
                onDelta.accept(delta);
            }
            return false;
        } catch (IOException | IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "千问流式响应格式无效，请点击重试", exception);
        }
    }

    private String streamContent(com.fasterxml.jackson.databind.JsonNode root) {
        if (root == null || root.isNull()) {
            return null;
        }
        com.fasterxml.jackson.databind.JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            choices = root.path("output").path("choices");
        }
        if (!choices.isArray() || choices.isEmpty()) {
            return null;
        }
        com.fasterxml.jackson.databind.JsonNode choice = choices.get(0);
        String delta = textContent(choice.path("delta").path("content"));
        if (delta != null) {
            return delta;
        }
        return textContent(choice.path("message").path("content"));
    }

    private String textContent(com.fasterxml.jackson.databind.JsonNode node) {
        return node != null && node.isTextual() ? node.textValue() : null;
    }

    private boolean isNativeStreamingUnsupported(int status) {
        return status == 400 || status == 404 || status == 405 || status == 406
                || status == 415 || status == 422;
    }

    public record RecipeStreamResult(
            String content,
            RecipeGenerateResponse fallbackResponse,
            String provider,
            String model,
            boolean nativeStreaming
    ) {
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
            return objectMapper.treeToValue(objectMapper.readTree(stripJsonFence(content)), RecipePayload.class);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "千问返回内容不是有效菜谱 JSON", exception);
        }
    }

    private List<WeeklyMenuSelection> parseWeeklyMenu(QwenChatResponse response) {
        String content = firstContent(response);
        try {
            com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(stripJsonFence(content));
            if (node == null || node.isNull() || (!node.isArray() && !node.isObject())) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "千问返回的周菜单不是有效 JSON");
            }
            if (node.isArray()) {
                return objectMapper.convertValue(node, objectMapper.getTypeFactory()
                        .constructCollectionType(List.class, WeeklyMenuSelection.class));
            }
            WeeklyMenuPayload payload = objectMapper.treeToValue(node, WeeklyMenuPayload.class);
            return payload.items() == null ? List.of() : List.copyOf(payload.items());
        } catch (IOException | IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "千问返回的周菜单不是有效 JSON", exception);
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
            List<RecipeGenerateResponse.MissingIngredient> missingIngredients,
            List<RecipeGenerateResponse.Step> steps,
            List<String> tips,
            List<String> videoKeywords,
            RecipeGenerateResponse.Explanation explanation,
            com.fasterxml.jackson.databind.JsonNode nutritionEstimate
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record WeeklyMenuPayload(List<WeeklyMenuSelection> items) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WeeklyMenuSelection(
            String menuDate,
            String mealType,
            Long recipeId
    ) {
    }
}
