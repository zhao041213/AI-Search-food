package com.example.food.ai.qwen;

import com.example.food.ai.recipe.dto.RecipeGenerateResponse;
import com.example.food.ai.config.AiModelConfigService;
import com.example.food.ai.config.AiModelRuntimeConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class QwenRecipeClientTest {

    @Test
    void generateRecipeCallsQwenAndParsesStructuredContent() {
        RestTemplate restTemplate = new RestTemplateBuilder().build();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        QwenProperties properties = new QwenProperties(
                "test-api-key",
                "qwen-plus",
                "https://dashscope.test/compatible-mode/v1/chat/completions"
        );
        QwenRecipeClient client = new QwenRecipeClient(restTemplate, new ObjectMapper(), properties);

        server.expect(once(), requestTo(properties.endpoint()))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-api-key"))
                .andExpect(jsonPath("$.model").value("qwen-plus"))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andRespond(withSuccess("""
                        {
                          "choices": [
                            {
                              "message": {
                                "content": "{\\"title\\":\\"番茄炒蛋\\",\\"summary\\":\\"家常快手菜\\",\\"effects\\":[\\"补充蛋白质\\"],\\"ingredients\\":[{\\"name\\":\\"番茄\\",\\"amount\\":\\"2个\\"}],\\"steps\\":[{\\"order\\":1,\\"title\\":\\"备菜\\",\\"description\\":\\"番茄切块。\\",\\"durationMinutes\\":5}],\\"tips\\":[\\"先炒鸡蛋\\"],\\"videoKeywords\\":[\\"番茄炒蛋 做法\\"]}"
                              }
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        RecipeGenerateResponse response = client.generateRecipe("请根据番茄和鸡蛋生成菜谱");

        assertThat(response.title()).isEqualTo("番茄炒蛋");
        assertThat(response.effects()).containsExactly("补充蛋白质");
        assertThat(response.ingredients()).extracting(RecipeGenerateResponse.Ingredient::name).containsExactly("番茄");
        assertThat(response.steps()).extracting(RecipeGenerateResponse.Step::title).containsExactly("备菜");
        assertThat(response.provider()).isEqualTo("qwen");
        assertThat(response.model()).isEqualTo("qwen-plus");
        server.verify();
    }

    @Test
    void generateRecipeUsesRuntimeConfigWhenAdminSavedIt() {
        RestTemplate restTemplate = new RestTemplateBuilder().build();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        QwenProperties properties = new QwenProperties(
                "env-api-key",
                "qwen-plus",
                "https://dashscope.env/compatible-mode/v1/chat/completions"
        );
        AiModelConfigService configService = mock(AiModelConfigService.class);
        when(configService.textRecipeRuntimeConfig()).thenReturn(new AiModelRuntimeConfig(
                "qwen",
                "qwen-max",
                "https://dashscope.admin/compatible-mode/v1/chat/completions",
                "admin-api-key"
        ));
        QwenRecipeClient client = new QwenRecipeClient(restTemplate, new ObjectMapper(), properties, configService);

        server.expect(once(), requestTo("https://dashscope.admin/compatible-mode/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer admin-api-key"))
                .andExpect(jsonPath("$.model").value("qwen-max"))
                .andRespond(withSuccess("""
                        {
                          "choices": [
                            {
                              "message": {
                                "content": "{\\"title\\":\\"青椒肉丝\\",\\"summary\\":\\"家常下饭菜\\",\\"effects\\":[\\"补充能量\\"],\\"ingredients\\":[{\\"name\\":\\"青椒\\",\\"amount\\":\\"2个\\"}],\\"steps\\":[{\\"order\\":1,\\"title\\":\\"切配\\",\\"description\\":\\"青椒切丝。\\",\\"durationMinutes\\":5}],\\"tips\\":[\\"大火快炒\\"],\\"videoKeywords\\":[\\"青椒肉丝 做法\\"]}"
                              }
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        RecipeGenerateResponse response = client.generateRecipe("请根据青椒和猪肉生成菜谱");

        assertThat(response.title()).isEqualTo("青椒肉丝");
        assertThat(response.model()).isEqualTo("qwen-max");
        server.verify();
    }
}
