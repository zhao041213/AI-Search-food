package com.example.food.service;

import org.mockito.ArgumentMatchers;
import org.springframework.core.ParameterizedTypeReference;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeepSeekRecipeServiceTest {

    @Test
    void generateRecipeSendsBearerTokenAndExtractsAssistantContent() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        DeepSeekRecipeService service = new DeepSeekRecipeService(
                restTemplate,
                "test-key",
                "https://api.deepseek.com/chat/completions",
                "deepseek-v4-flash"
        );

        Map<String, Object> response = Map.of(
                "choices", List.of(Map.of(
                        "message", Map.of("content", "番茄炒蛋做法：先炒鸡蛋，再炒番茄。")
                ))
        );
        when(restTemplate.exchange(
                eq("https://api.deepseek.com/chat/completions"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any()
        )).thenReturn(ResponseEntity.ok(response));

        String content = service.generateRecipe("番茄, 鸡蛋");

        assertThat(content).contains("番茄炒蛋");

        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(
                eq("https://api.deepseek.com/chat/completions"),
                eq(HttpMethod.POST),
                entityCaptor.capture(),
                ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any()
        );

        HttpEntity<?> entity = entityCaptor.getValue();
        assertThat(entity.getHeaders().getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer test-key");
        assertThat(entity.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE)).isEqualTo("application/json");

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) entity.getBody();
        assertThat(body).containsEntry("model", "deepseek-v4-flash");
        assertThat(body).containsEntry("stream", false);
        assertThat(body).containsKey("messages");
    }

    @Test
    void generateRecipeRemovesSurroundingQuotesFromApiKey() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        DeepSeekRecipeService service = new DeepSeekRecipeService(
                restTemplate,
                "\"test-key\"",
                "https://api.deepseek.com/chat/completions",
                "deepseek-v4-flash"
        );

        Map<String, Object> response = Map.of(
                "choices", List.of(Map.of(
                        "message", Map.of("content", "recipe content")
                ))
        );
        when(restTemplate.exchange(
                eq("https://api.deepseek.com/chat/completions"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any()
        )).thenReturn(ResponseEntity.ok(response));

        service.generateRecipe("egg");

        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(
                eq("https://api.deepseek.com/chat/completions"),
                eq(HttpMethod.POST),
                entityCaptor.capture(),
                ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any()
        );

        assertThat(entityCaptor.getValue().getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
                .isEqualTo("Bearer test-key");
    }

    @Test
    void generateRecipeThrowsConfigurationExceptionWhenApiKeyIsMissing() {
        DeepSeekRecipeService service = new DeepSeekRecipeService(
                mock(RestTemplate.class),
                " ",
                "https://api.deepseek.com/chat/completions",
                "deepseek-v4-flash"
        );

        assertThatThrownBy(() -> service.generateRecipe("鸡蛋"))
                .isInstanceOf(DeepSeekConfigurationException.class)
                .hasMessageContaining("DEEPSEEK_API_KEY");
    }
}
