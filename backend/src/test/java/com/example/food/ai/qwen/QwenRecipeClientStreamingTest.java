package com.example.food.ai.qwen;

import com.example.food.ai.config.AiModelConfigService;
import com.example.food.ai.config.AiModelRuntimeConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class QwenRecipeClientStreamingTest {

    private static final String ENDPOINT = "https://dashscope.test/compatible-mode/v1/chat/completions";

    @Test
    void readsOpenAiCompatibleSseAndRequiresDone() {
        RestTemplate restTemplate = new RestTemplateBuilder().build();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        QwenRecipeClient client = client(restTemplate);
        AtomicReference<String> content = new AtomicReference<>("");

        server.expect(once(), requestTo(ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.stream").value(true))
                .andRespond(withSuccess(
                        "data: {\"choices\":[{\"delta\":{\"content\":\"{\\\"title\\\":\\\"番\"}}]}\n\n"
                                + "data: {\"choices\":[{\"delta\":{\"content\":\"茄\\\"}\"}}]}\n\n"
                                + "data: [DONE]\n\n",
                        MediaType.TEXT_EVENT_STREAM
                ));

        QwenRecipeClient.RecipeStreamResult result = client.streamRecipe(
                "请生成菜谱",
                delta -> content.updateAndGet(existing -> existing + delta),
                () -> { }
        );

        assertThat(result.nativeStreaming()).isTrue();
        assertThat(result.content()).contains("番");
        assertThat(content.get()).isEqualTo(result.content());
        server.verify();
    }

    @Test
    void fallsBackToOrdinaryGenerationWhenProviderRejectsNativeStream() throws Exception {
        RestTemplate restTemplate = new RestTemplateBuilder().build();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        QwenRecipeClient client = client(restTemplate);
        String recipeJson = "{\"title\":\"番茄炒蛋\",\"summary\":\"家常菜\","
                + "\"ingredients\":[{\"name\":\"番茄\",\"amount\":\"2个\"}],"
                + "\"steps\":[{\"order\":1,\"title\":\"炒制\",\"description\":\"炒熟\"}]}";

        server.expect(once(), requestTo(ENDPOINT))
                .andExpect(jsonPath("$.stream").value(true))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));
        server.expect(once(), requestTo(ENDPOINT))
                .andExpect(jsonPath("$.stream").doesNotExist())
                .andRespond(withSuccess(
                        "{\"choices\":[{\"message\":{\"content\":"
                                + new ObjectMapper().writeValueAsString(recipeJson) + "}}]}",
                        MediaType.APPLICATION_JSON
                ));

        QwenRecipeClient.RecipeStreamResult result = client.streamRecipe("请生成菜谱", delta -> { }, () -> { });

        assertThat(result.nativeStreaming()).isFalse();
        assertThat(result.fallbackResponse()).isNotNull();
        assertThat(result.fallbackResponse().title()).isEqualTo("番茄炒蛋");
        server.verify();
    }

    @Test
    void rejectsInterruptedSseWithoutDoneMarker() {
        RestTemplate restTemplate = new RestTemplateBuilder().build();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        QwenRecipeClient client = client(restTemplate);

        server.expect(once(), requestTo(ENDPOINT))
                .andRespond(withSuccess(
                        "data: {\"choices\":[{\"delta\":{\"content\":\"{\\\"title\\\":\\\"番\"}}]}\n\n",
                        MediaType.TEXT_EVENT_STREAM
                ));

        assertThatThrownBy(() -> client.streamRecipe("请生成菜谱", delta -> { }, () -> { }))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("流式响应中断");
        server.verify();
    }

    @Test
    void readsAnthropicCompatibleSseAndMessageStop() {
        RestTemplate restTemplate = new RestTemplateBuilder().build();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        AiModelConfigService configService = mock(AiModelConfigService.class);
        when(configService.textRecipeRuntimeConfig()).thenReturn(new AiModelRuntimeConfig(
                "qwen",
                "anthropic",
                "qwen-plus",
                "https://dashscope.test/apps/anthropic",
                "anthropic-api-key"
        ));
        QwenRecipeClient client = new QwenRecipeClient(
                restTemplate,
                new ObjectMapper(),
                new QwenProperties("env-api-key", "qwen-plus", ENDPOINT),
                configService
        );
        AtomicReference<String> content = new AtomicReference<>("");

        server.expect(once(), requestTo("https://dashscope.test/apps/anthropic"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.stream").value(true))
                .andRespond(withSuccess(
                        "event: message_start\n"
                                + "data: {\"type\":\"message_start\"}\n\n"
                                + "event: content_block_delta\n"
                                + "data: {\"type\":\"content_block_delta\",\"delta\":{\"type\":\"text_delta\",\"text\":\"{\\\"title\\\":\\\"清蒸螃蟹\\\"}\"}}\n\n"
                                + "event: message_stop\n"
                                + "data: {\"type\":\"message_stop\"}\n\n",
                        MediaType.TEXT_EVENT_STREAM
                ));

        QwenRecipeClient.RecipeStreamResult result = client.streamRecipe(
                "请生成菜谱",
                delta -> content.updateAndGet(existing -> existing + delta),
                () -> { }
        );

        assertThat(result.nativeStreaming()).isTrue();
        assertThat(result.content()).contains("清蒸螃蟹");
        assertThat(content.get()).isEqualTo(result.content());
        server.verify();
    }

    private QwenRecipeClient client(RestTemplate restTemplate) {
        return new QwenRecipeClient(
                restTemplate,
                new ObjectMapper(),
                new QwenProperties("test-api-key", "qwen-plus", ENDPOINT)
        );
    }
}
