package com.example.food.recipe;

import com.example.food.recipe.dto.RecipeHistoryDetailResponse;
import com.example.food.recipe.dto.RecipeHistorySummaryResponse;
import com.example.food.recipe.dto.SaveRecipeRequest;
import com.example.food.security.AppRole;
import com.example.food.security.AuthPrincipal;
import com.example.food.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SavedRecipeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SavedRecipeService savedRecipeService;

    @Test
    void savedRecipeListRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/recipes/saved"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void userCanReadSavedRecipeList() throws Exception {
        when(savedRecipeService.list(7L, null, null, null, 50, 0)).thenReturn(List.of(
                new RecipeHistorySummaryResponse(99L, "番茄炒蛋", "番茄、鸡蛋", "dinner", "balanced", null)
        ));
        String token = jwtService.generateToken(new AuthPrincipal(7L, "13800138000", AppRole.USER));

        mockMvc.perform(get("/api/recipes/saved")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].id").value(99))
                .andExpect(jsonPath("$.data[0].title").value("番茄炒蛋"));
    }

    @Test
    void adminCannotReadUserSavedRecipeList() throws Exception {
        String token = jwtService.generateToken(new AuthPrincipal(1L, "admin", AppRole.ADMIN));

        mockMvc.perform(get("/api/recipes/saved")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void userCanSearchAndFilterSavedRecipeList() throws Exception {
        when(savedRecipeService.list(7L, "番茄", "dinner", "balanced", 10, 5))
                .thenReturn(List.of());
        String token = jwtService.generateToken(new AuthPrincipal(7L, "13800138000", AppRole.USER));

        mockMvc.perform(get("/api/recipes/saved")
                        .header("Authorization", "Bearer " + token)
                        .param("keyword", "番茄")
                        .param("mealType", "dinner")
                        .param("goal", "balanced")
                        .param("limit", "10")
                        .param("offset", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(savedRecipeService).list(7L, "番茄", "dinner", "balanced", 10, 5);
    }

    @Test
    void userCanSaveRecipe() throws Exception {
        when(savedRecipeService.save(any(SaveRecipeRequest.class), any(AuthPrincipal.class), any()))
                .thenReturn(new RecipeHistoryDetailResponse(99L, 10L, null, "番茄、鸡蛋", "dinner", "balanced", null));
        String token = jwtService.generateToken(new AuthPrincipal(7L, "13800138000", AppRole.USER));

        mockMvc.perform(post("/api/recipes/saved")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Anonymous-Id", "anonymous-12345678")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SaveRecipeRequest(
                                10L,
                                "番茄炒蛋",
                                "家常快手菜",
                                List.of("补充蛋白质"),
                                List.of(),
                                List.of(),
                                List.of(),
                                List.of(),
                                "qwen",
                                "qwen-plus"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void userCanDeleteOwnedSavedRecipe() throws Exception {
        String token = jwtService.generateToken(new AuthPrincipal(7L, "13800138000", AppRole.USER));

        mockMvc.perform(delete("/api/recipes/saved/99")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(savedRecipeService).delete(7L, 99L);
    }

    @Test
    void deletingMissingOrOtherUsersSavedRecipeReturnsNotFound() throws Exception {
        doThrow(new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND, "菜谱不存在"))
                .when(savedRecipeService).delete(7L, 99L);
        String token = jwtService.generateToken(new AuthPrincipal(7L, "13800138000", AppRole.USER));

        mockMvc.perform(delete("/api/recipes/saved/99")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }
}
