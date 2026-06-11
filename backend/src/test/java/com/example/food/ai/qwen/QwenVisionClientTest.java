package com.example.food.ai.qwen;

import com.example.food.ai.ingredient.dto.IngredientRecognitionResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class QwenVisionClientTest {

    @Test
    void recognizeIngredientsSendsImageUrlContentAndParsesStructuredResult() {
        RestTemplate restTemplate = new RestTemplateBuilder().build();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        QwenProperties properties = new QwenProperties(
                "test-api-key",
                "qwen-plus",
                "https://dashscope.test/compatible-mode/v1/chat/completions"
        );
        QwenVisionClient client = new QwenVisionClient(restTemplate, new ObjectMapper(), properties);

        server.expect(once(), requestTo(properties.endpoint()))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-api-key"))
                .andExpect(jsonPath("$.model").value("qwen-vl-plus"))
                .andExpect(jsonPath("$.messages[1].content[0].type").value("text"))
                .andExpect(jsonPath("$.messages[1].content[1].type").value("image_url"))
                .andExpect(jsonPath("$.messages[1].content[1].image_url.url", startsWith("data:image/png;base64,")))
                .andRespond(withSuccess("""
                        {
                          "choices": [
                            {
                              "message": {
                                "content": "{\\"ingredients\\":[\\"番茄\\",\\"鸡蛋\\"],\\"description\\":\\"图片中可见番茄和鸡蛋。\\"}"
                              }
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        IngredientRecognitionResponse response = client.recognizeIngredients("image/png", new byte[]{1, 2, 3});

        assertThat(response.ingredients()).containsExactly("番茄", "鸡蛋");
        assertThat(response.description()).isEqualTo("图片中可见番茄和鸡蛋。");
        assertThat(response.provider()).isEqualTo("qwen");
        assertThat(response.model()).isEqualTo("qwen-vl-plus");
        server.verify();
    }
}
