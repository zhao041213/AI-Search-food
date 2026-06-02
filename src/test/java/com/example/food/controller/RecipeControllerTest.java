package com.example.food.controller;

import com.example.food.service.DeepSeekRecipeService;
import com.example.food.service.RecipeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecipeController.class)
class RecipeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RecipeService recipeService;

    @MockBean
    private DeepSeekRecipeService deepSeekRecipeService;

    @Test
    void generateReturnsBadRequestWhenIngredientsAreBlank() throws Exception {
        mockMvc.perform(post("/api/recipes/generate")
                        .contentType("application/json")
                        .content("{\"ingredients\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("请输入食材"));
    }
}
