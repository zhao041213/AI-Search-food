package com.example.food.user.nutrition;

import com.example.food.common.GlobalExceptionHandler;
import com.example.food.security.AppRole;
import com.example.food.security.AuthPrincipal;
import com.example.food.security.JwtService;
import com.example.food.security.SecurityConfig;
import com.example.food.user.nutrition.dto.NutritionTargetResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserNutritionTargetController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class UserNutritionTargetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserNutritionTargetService service;

    @Test
    void readsOnlyTheAuthenticatedUsersTarget() throws Exception {
        authenticateUser();
        when(service.get(7L)).thenReturn(new NutritionTargetResponse(
                true, true, new BigDecimal("2000"), new BigDecimal("80"),
                new BigDecimal("60"), new BigDecimal("260"), null
        ));

        mockMvc.perform(get("/api/users/me/nutrition-target")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.configured").value(true))
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.caloriesKcal").value(2000));

        verify(service).get(7L);
    }

    @Test
    void validatesEnabledTargetBeforeSaving() throws Exception {
        authenticateUser();
        when(service.save(eq(7L), any())).thenThrow(new IllegalArgumentException("请输入每日碳水目标"));

        mockMvc.perform(put("/api/users/me/nutrition-target")
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "enabled": true,
                                  "caloriesKcal": 2000,
                                  "proteinG": 80,
                                  "fatG": 60
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请输入每日碳水目标"));
    }

    @Test
    void deleteUsesAuthenticatedUserIdAndIsIdempotentAtTheApiBoundary() throws Exception {
        authenticateUser();

        mockMvc.perform(delete("/api/users/me/nutrition-target")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(service).delete(7L);
    }

    private void authenticateUser() {
        when(jwtService.parseToken("user-token"))
                .thenReturn(new AuthPrincipal(7L, "13800138000", AppRole.USER));
    }
}
