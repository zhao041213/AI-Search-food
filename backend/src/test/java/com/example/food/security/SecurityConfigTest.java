package com.example.food.security;

import com.example.food.common.ApiResponse;
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

@WebMvcTest(controllers = SecurityConfigTest.TestController.class)
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtService jwtService;

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
