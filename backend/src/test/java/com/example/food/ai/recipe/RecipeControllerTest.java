package com.example.food.ai.recipe;

import com.example.food.ai.recipe.dto.RecipeGenerateRequest;
import com.example.food.ai.recipe.dto.RecipeGenerateResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecipeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RecipeRecommendationService recipeRecommendationService;

    @Test
    void generateRecipeReturnsStructuredRecipe() throws Exception {
        RecipeGenerateResponse response = new RecipeGenerateResponse(
                "番茄炒蛋",
                "适合家常快手晚餐的菜谱。",
                List.of("补充蛋白质", "开胃下饭"),
                List.of(new RecipeGenerateResponse.Ingredient("番茄", "2个")),
                List.of(new RecipeGenerateResponse.MissingIngredient(
                        "鸡蛋",
                        "2个",
                        List.of("嫩豆腐"),
                        "用户已有食材中没有鸡蛋"
                )),
                List.of(new RecipeGenerateResponse.Step(1, "备菜", "番茄切块，鸡蛋打散。", 5)),
                List.of("先炒鸡蛋再炒番茄，口感更嫩。"),
                List.of("番茄炒蛋 家常 做法"),
                new RecipeGenerateResponse.Explanation(
                        "番茄的酸甜与鸡蛋的鲜香互补",
                        "包含蛋白质与维生素",
                        "分开炒制有利于保持嫩度"
                ),
                "qwen",
                "qwen-plus"
        );
        response = response.withSearchLogId(42L);
        when(recipeRecommendationService.generate(
                any(RecipeGenerateRequest.class),
                isNull(),
                anyString()
        )).thenReturn(response);

        mockMvc.perform(post("/api/ai/recipes/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Anonymous-Id", "anonymous-12345678")
                        .content("""
                                {
                                  "ingredients": "番茄、鸡蛋",
                                  "mealType": "dinner",
                                  "goal": "balanced",
                                  "searchMode": "text",
                                  "regenerationPreference": "减少用油",
                                  "previousTitle": "番茄炒蛋（上一版）"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.title").value("番茄炒蛋"))
                .andExpect(jsonPath("$.data.effects[0]").value("补充蛋白质"))
                .andExpect(jsonPath("$.data.ingredients[0].name").value("番茄"))
                .andExpect(jsonPath("$.data.missingIngredients[0].name").value("鸡蛋"))
                .andExpect(jsonPath("$.data.missingIngredients[0].substitutes[0]").value("嫩豆腐"))
                .andExpect(jsonPath("$.data.steps[0].title").value("备菜"))
                .andExpect(jsonPath("$.data.explanation.pairingLogic").value("番茄的酸甜与鸡蛋的鲜香互补"))
                .andExpect(jsonPath("$.data.provider").value("qwen"))
                .andExpect(jsonPath("$.data.searchLogId").value(42));

        ArgumentCaptor<RecipeGenerateRequest> requestCaptor = ArgumentCaptor.forClass(RecipeGenerateRequest.class);
        verify(recipeRecommendationService).generate(requestCaptor.capture(), isNull(), anyString());
        assertThat(requestCaptor.getValue().regenerationPreference()).isEqualTo("减少用油");
        assertThat(requestCaptor.getValue().previousTitle()).isEqualTo("番茄炒蛋（上一版）");
    }

    @Test
    void generateRecipeRejectsBlankIngredients() throws Exception {
        mockMvc.perform(post("/api/ai/recipes/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RecipeGenerateRequest("", "dinner", "balanced", "text"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }
}
