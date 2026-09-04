package com.example.food.recipe.collection;

import com.example.food.recipe.collection.dto.RecipeCollectionResponse;
import com.example.food.recipe.collection.dto.SavedRecipePageResponse;
import com.example.food.recipe.collection.dto.SavedRecipeResponse;
import com.example.food.security.AppRole;
import com.example.food.security.AuthPrincipal;
import com.example.food.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SavedRecipeCollectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private SavedRecipeCollectionService service;

    @Test
    void enhancedSavedRecipeListRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/users/me/saved-recipes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void userCanReadCollectionsAndFilteredSavedRecipes() throws Exception {
        LocalDateTime savedAt = LocalDateTime.of(2026, 9, 4, 12, 0);
        when(service.listCollections(7L)).thenReturn(List.of(
                new RecipeCollectionResponse(1L, "默认收藏夹", true, 1, savedAt, savedAt)
        ));
        when(service.listSavedRecipes(7L, 1L, "番茄", "dinner", "balanced", "家常", "savedAtDesc", 1, 20))
                .thenReturn(new SavedRecipePageResponse(
                        List.of(new SavedRecipeResponse(
                                99L, "番茄炒蛋", "家常快手菜", "番茄、鸡蛋", "dinner", "balanced", "番茄",
                                savedAt, 1L, "默认收藏夹", List.of("家常")
                        )),
                        1,
                        1,
                        20,
                        1
                ));
        String token = jwtService.generateToken(new AuthPrincipal(7L, "13800138000", AppRole.USER));

        mockMvc.perform(get("/api/users/me/recipe-collections")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].defaultCollection").value(true));

        mockMvc.perform(get("/api/users/me/saved-recipes")
                        .header("Authorization", "Bearer " + token)
                        .param("collectionId", "1")
                        .param("keyword", "番茄")
                        .param("mealType", "dinner")
                        .param("goal", "balanced")
                        .param("tag", "家常")
                        .param("sort", "savedAtDesc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].collectionName").value("默认收藏夹"))
                .andExpect(jsonPath("$.data.items[0].tags[0]").value("家常"));

        verify(service).listSavedRecipes(eq(7L), eq(1L), eq("番茄"), eq("dinner"), eq("balanced"), eq("家常"), eq("savedAtDesc"), eq(1), eq(20));
    }
}
