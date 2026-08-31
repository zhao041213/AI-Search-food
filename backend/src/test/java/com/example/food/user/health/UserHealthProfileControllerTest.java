package com.example.food.user.health;

import com.example.food.common.GlobalExceptionHandler;
import com.example.food.security.AppRole;
import com.example.food.security.AuthPrincipal;
import com.example.food.security.JwtService;
import com.example.food.security.SecurityConfig;
import com.example.food.user.health.dto.HealthProfileResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserHealthProfileController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class UserHealthProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserHealthProfileService service;

    @Test
    void userCanReadOwnProfile() throws Exception {
        authenticateUser();
        when(service.get(7L)).thenReturn(response());

        mockMvc.perform(get("/api/users/me/health-profile")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.completed").value(true))
                .andExpect(jsonPath("$.data.ageRange").value("AGE_30_44"))
                .andExpect(jsonPath("$.data.bmi").value(21.6));
    }

    @Test
    void userCanSaveOwnProfile() throws Exception {
        authenticateUser();
        when(service.save(org.mockito.ArgumentMatchers.eq(7L), any())).thenReturn(response());

        mockMvc.perform(put("/api/users/me/health-profile")
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ageRange": "AGE_30_44",
                                  "heightCm": 172.0,
                                  "weightKg": 64.0,
                                  "activityLevel": "MODERATE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.completed").value(true))
                .andExpect(jsonPath("$.data.activityLevel").value("MODERATE"));

        verify(service).save(org.mockito.ArgumentMatchers.eq(7L), any());
    }

    @Test
    void invalidBodyMetricReturnsParameterError() throws Exception {
        authenticateUser();

        mockMvc.perform(put("/api/users/me/health-profile")
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ageRange": "AGE_30_44",
                                  "heightCm": 20,
                                  "weightKg": 64.0,
                                  "activityLevel": "MODERATE"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid request parameters"));
    }

    @Test
    void overPreciseBodyMetricReturnsParameterError() throws Exception {
        authenticateUser();

        mockMvc.perform(put("/api/users/me/health-profile")
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ageRange": "AGE_30_44",
                                  "heightCm": 172.12,
                                  "weightKg": 64.0,
                                  "activityLevel": "MODERATE"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid request parameters"));
    }

    @Test
    void userCanDeleteOwnProfile() throws Exception {
        authenticateUser();

        mockMvc.perform(delete("/api/users/me/health-profile")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(service).delete(7L);
    }

    private HealthProfileResponse response() {
        return new HealthProfileResponse(
                true,
                "AGE_30_44",
                new BigDecimal("172.0"),
                new BigDecimal("64.0"),
                "MODERATE",
                new BigDecimal("21.6"),
                null
        );
    }

    private void authenticateUser() {
        when(jwtService.parseToken("user-token"))
                .thenReturn(new AuthPrincipal(7L, "13800138000", AppRole.USER));
    }
}
