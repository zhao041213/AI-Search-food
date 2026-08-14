package com.example.food.security;

import com.example.food.common.ApiResponse;
import com.example.food.stats.HotIngredientStatsController;
import com.example.food.stats.HotIngredientStatsService;
import com.example.food.stats.dto.HotIngredientStatsResponse;
import com.example.food.user.preference.UserDietPreferenceController;
import com.example.food.user.preference.UserDietPreferenceService;
import com.example.food.user.preference.dto.DietPreferenceResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        SecurityConfigTest.TestController.class,
        HotIngredientStatsController.class,
        UserDietPreferenceController.class
})
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private HotIngredientStatsService hotIngredientStatsService;

    @MockBean
    private UserDietPreferenceService userDietPreferenceService;

    @Test
    void unauthenticatedProtectedEndpointReturnsJsonUnauthorized() throws Exception {
        mockMvc.perform(get("/api/user/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("Unauthorized"));
    }

    @Test
    void wrongRoleReturnsJsonForbidden() throws Exception {
        when(jwtService.parseToken("user-token"))
                .thenReturn(new AuthPrincipal(7L, "13800138000", AppRole.USER));

        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("Forbidden"));
    }

    @Test
    void invalidBearerTokenReturnsJsonUnauthorized() throws Exception {
        when(jwtService.parseToken("bad-token"))
                .thenThrow(new BadCredentialsException("Invalid JWT token"));

        mockMvc.perform(get("/api/user/profile")
                        .header("Authorization", "Bearer bad-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("Unauthorized"));
    }

    @Test
    void hotIngredientRankingIsPublic() throws Exception {
        when(hotIngredientStatsService.get("all", 10))
                .thenReturn(new HotIngredientStatsResponse("all", 0, 0, null, java.util.List.of()));

        mockMvc.perform(get("/api/stats/hot-ingredients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.period").value("all"));
    }

    @Test
    void unauthenticatedUserDietPreferenceRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/users/me/diet-preferences"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void adminCannotAccessUserDietPreferences() throws Exception {
        when(jwtService.parseToken("admin-token"))
                .thenReturn(new AuthPrincipal(1L, "admin", AppRole.ADMIN));

        mockMvc.perform(get("/api/users/me/diet-preferences")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void userCanAccessUserDietPreferences() throws Exception {
        when(jwtService.parseToken("user-token"))
                .thenReturn(new AuthPrincipal(7L, "13800138000", AppRole.USER));
        when(userDietPreferenceService.get(7L)).thenReturn(DietPreferenceResponse.empty());

        mockMvc.perform(get("/api/users/me/diet-preferences")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taste").value("any"));
    }

    @RestController
    static class TestController {

        @GetMapping("/api/user/profile")
        ApiResponse<String> userProfile() {
            return ApiResponse.ok("user");
        }

        @GetMapping("/api/admin/dashboard")
        ApiResponse<String> adminDashboard() {
            return ApiResponse.ok("admin");
        }

        @PostMapping(value = "/api/auth/user/login", consumes = MediaType.APPLICATION_JSON_VALUE)
        ApiResponse<String> userLogin() {
            return ApiResponse.ok("token");
        }
    }
}
