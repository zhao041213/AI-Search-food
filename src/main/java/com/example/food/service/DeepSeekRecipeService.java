package com.example.food.service;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class DeepSeekRecipeService {

    private static final ParameterizedTypeReference<Map<String, Object>> RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String apiUrl;
    private final String model;

    public DeepSeekRecipeService(
            RestTemplate restTemplate,
            @Value("${deepseek.api-key:}") String apiKey,
            @Value("${deepseek.api-url:https://api.deepseek.com/chat/completions}") String apiUrl,
            @Value("${deepseek.model:deepseek-v4-flash}") String model
    ) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.model = model;
    }

    public String generateRecipe(String ingredients) {
        if (!StringUtils.hasText(apiKey)) {
            throw new DeepSeekConfigurationException("请先配置环境变量 DEEPSEEK_API_KEY");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(normalizeApiKey(apiKey));

        Map<String, Object> body = Map.of(
                "model", model,
                "stream", false,
                "thinking", Map.of("type", "disabled"),
                "messages", List.of(
                        Map.of(
                                "role", "system",
                                "content", "你是一名专业中餐厨师。请根据用户给出的食材生成简单、可执行的中文菜谱。"
                        ),
                        Map.of(
                                "role", "user",
                                "content", buildPrompt(ingredients)
                        )
                )
        );

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    RESPONSE_TYPE
            );
            return extractContent(response.getBody());
        } catch (DeepSeekApiException | DeepSeekConfigurationException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new DeepSeekApiException("DeepSeek 调用失败：" + exception.getMessage(), exception);
        } catch (RuntimeException exception) {
            throw new DeepSeekApiException("DeepSeek 响应解析失败", exception);
        }
    }

    private String buildPrompt(String ingredients) {
        return """
                请基于这些食材生成一道适合家常制作的菜谱：%s。
                输出格式：
                1. 菜名
                2. 所需食材
                3. 制作步骤
                4. 小贴士
                要求步骤清晰，适合新手操作。
                """.formatted(ingredients.trim());
    }

    private String normalizeApiKey(String value) {
        String trimmedValue = value.trim();
        if (trimmedValue.length() >= 2
                && ((trimmedValue.startsWith("\"") && trimmedValue.endsWith("\""))
                || (trimmedValue.startsWith("'") && trimmedValue.endsWith("'")))) {
            return trimmedValue.substring(1, trimmedValue.length() - 1);
        }
        return trimmedValue;
    }

    private String extractContent(Map<String, Object> responseBody) {
        if (responseBody == null) {
            throw new DeepSeekApiException("DeepSeek 响应为空");
        }

        Object choicesValue = responseBody.get("choices");
        if (!(choicesValue instanceof List<?> choices) || choices.isEmpty()) {
            throw new DeepSeekApiException("DeepSeek 响应缺少 choices");
        }

        Object firstChoice = choices.get(0);
        if (!(firstChoice instanceof Map<?, ?> choice)) {
            throw new DeepSeekApiException("DeepSeek 响应 choices 格式错误");
        }

        Object messageValue = choice.get("message");
        if (!(messageValue instanceof Map<?, ?> message)) {
            throw new DeepSeekApiException("DeepSeek 响应缺少 message");
        }

        Object contentValue = message.get("content");
        if (!(contentValue instanceof String content) || !StringUtils.hasText(content)) {
            throw new DeepSeekApiException("DeepSeek 响应缺少 content");
        }

        return content.trim();
    }
}
