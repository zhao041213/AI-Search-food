package com.example.food.recipe.share;

import com.example.food.recipe.share.dto.PublicRecipeResponse;
import com.example.food.recipe.share.dto.RecipeShareResponse;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecipeShareControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private RecipeShareService service;

    @Test
    void publicShareCanBeReadWithoutAuthentication() throws Exception {
        when(service.publicRecipe("share-token")).thenReturn(new PublicRecipeResponse(
                "番茄炒蛋",
                "家常快手菜",
                List.of(new PublicRecipeResponse.Ingredient("番茄", "2 个")),
                List.of(new PublicRecipeResponse.Step(1, "准备食材", "切好番茄", 5)),
                List.of("出锅前调味"),
                new PublicRecipeResponse.Nutrition(2, new BigDecimal("420"), new BigDecimal("24"),
                        new BigDecimal("16"), new BigDecimal("42"))
        ));

        mockMvc.perform(get("/api/shared/recipes/share-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("番茄炒蛋"))
                .andExpect(jsonPath("$.data.ingredients[0].name").value("番茄"));
    }

    @Test
    void authenticatedUserCanReadShareRecords() throws Exception {
        LocalDateTime createdAt = LocalDateTime.of(2026, 9, 4, 12, 0);
        when(service.listShares(7L)).thenReturn(List.of(new RecipeShareResponse(
                1L, 99L, "番茄炒蛋", "share-token", "/shared/recipes/share-token", "DAY_7",
                createdAt.plusDays(7), null, "ACTIVE", createdAt
        )));
        String token = jwtService.generateToken(new AuthPrincipal(7L, "13800138000", AppRole.USER));

        mockMvc.perform(get("/api/users/me/recipe-shares")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("ACTIVE"));
    }
}
