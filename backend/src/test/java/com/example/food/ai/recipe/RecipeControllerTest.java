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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
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
                List.of(new RecipeGenerateResponse.Step(1, "备菜", "番茄切块，鸡蛋打散。", 5)),
                List.of("先炒鸡蛋再炒番茄，口感更嫩。"),
                List.of("番茄炒蛋 家常 做法"),
                "qwen",
                "qwen-plus"
        );
        when(recipeRecommendationService.generate(any(RecipeGenerateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/ai/recipes/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ingredients": "番茄、鸡蛋",
                                  "mealType": "dinner",
                                  "goal": "balanced",
                                  "searchMode": "text"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.title").value("番茄炒蛋"))
                .andExpect(jsonPath("$.data.effects[0]").value("补充蛋白质"))
                .andExpect(jsonPath("$.data.ingredients[0].name").value("番茄"))
                .andExpect(jsonPath("$.data.steps[0].title").value("备菜"))
                .andExpect(jsonPath("$.data.provider").value("qwen"));
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
