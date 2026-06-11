package com.example.food.ai.ingredient;

import com.example.food.ai.ingredient.dto.IngredientRecognitionResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IngredientRecognitionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IngredientRecognitionService ingredientRecognitionService;

    @Test
    void recognizeIngredientsFromUploadedImage() throws Exception {
        when(ingredientRecognitionService.recognize(any())).thenReturn(new IngredientRecognitionResponse(
                List.of("番茄", "鸡蛋"),
                "图片中可见番茄和鸡蛋。",
                "qwen",
                "qwen-vl-plus"
        ));
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "ingredients.png",
                "image/png",
                new byte[]{1, 2, 3}
        );

        mockMvc.perform(multipart("/api/ai/ingredients/recognize").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.ingredients[0]").value("番茄"))
                .andExpect(jsonPath("$.data.ingredients[1]").value("鸡蛋"))
                .andExpect(jsonPath("$.data.description").value("图片中可见番茄和鸡蛋。"))
                .andExpect(jsonPath("$.data.provider").value("qwen"))
                .andExpect(jsonPath("$.data.model").value("qwen-vl-plus"));
    }
}
