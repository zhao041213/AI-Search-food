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

    @Test
    void verifyIngredientImageSendsTargetIngredientAndParsesVerification() {
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
                .andExpect(jsonPath("$.messages[1].content[0].text", startsWith("请判断图片中是否清晰展示了名为“番茄”")))
                .andRespond(withSuccess("""
                        {
                          "choices": [
                            {
                              "message": {
                                "content": "{\\"matches\\":true,\\"confidence\\":0.93,\\"description\\":\\"图片主体为番茄\\"}"
                              }
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        QwenVisionClient.IngredientImageVerification response =
                client.verifyIngredientImage("image/jpeg", new byte[]{4, 5, 6}, "番茄");

        assertThat(response.matches()).isTrue();
        assertThat(response.confidence()).isEqualTo(0.93d);
        assertThat(response.description()).isEqualTo("图片主体为番茄");
        server.verify();
    }

    @Test
    void reviewFinishedDishParsesStructuredCookingFeedback() {
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
                .andExpect(jsonPath("$.messages[1].content[0].text", startsWith("请结合成品照片和菜谱上下文")))
                .andRespond(withSuccess("""
                        {
                          "choices": [
                            {
                              "message": {
                                "content": "{\\"overallScore\\":86,\\"summary\\":\\"成品色泽自然，整体完成度较好\\",\\"color\\":{\\"score\\":88,\\"comment\\":\\"番茄颜色明亮\\"},\\"doneness\\":{\\"score\\":82,\\"comment\\":\\"鸡蛋火候合适\\"},\\"plating\\":{\\"score\\":80,\\"comment\\":\\"盘边可以更整洁\\"},\\"strengths\\":[\\"食材层次清晰\\"],\\"issues\\":[{\\"severity\\":\\"low\\",\\"title\\":\\"汤汁略多\\",\\"evidence\\":\\"盘底可见少量水分\\",\\"suggestion\\":\\"番茄下锅后缩短翻炒时间\\"}]}"
                              }
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        QwenVisionClient.FinishedDishReview response = client.reviewFinishedDish(
                "image/jpeg",
                new byte[]{7, 8, 9},
                new QwenVisionClient.FinishedDishReviewContext(
                        "番茄炒蛋",
                        java.util.List.of("番茄", "鸡蛋"),
                        java.util.List.of("炒熟鸡蛋", "加入番茄翻炒")
                )
        );

        assertThat(response.overallScore()).isEqualTo(86);
        assertThat(response.color().score()).isEqualTo(88);
        assertThat(response.issues()).singleElement()
                .extracting(QwenVisionClient.FinishedDishIssue::suggestion)
                .isEqualTo("番茄下锅后缩短翻炒时间");
        assertThat(response.safetyNote()).contains("视觉烹饪建议");
        server.verify();
    }
}
