package com.example.food.admin.operation;

import com.example.food.security.AppRole;
import com.example.food.security.AuthPrincipal;
import com.example.food.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminOperationLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Test
    void adminCanQueryOperationLogsAndSensitiveValuesAreNotReturned() throws Exception {
        String adminToken = token(1L, "admin", AppRole.ADMIN);

        mockMvc.perform(get("/api/admin/dashboard/overview")
                        .param("period", "7d")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("X-Forwarded-For", "10.0.0.8, 10.0.0.9"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/operation-logs")
                        .param("keyword", "VIEW_DASHBOARD")
                        .param("result", "SUCCESS")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(greaterThan(0)))
                .andExpect(jsonPath("$.data.items[*].operationType").value(hasItem("VIEW_DASHBOARD")))
                .andExpect(content().string(not(containsString("password"))))
                .andExpect(content().string(not(containsString("apiKey"))))
                .andExpect(content().string(not(containsString("rawResponse"))));
    }

    @Test
    void userCannotQueryAdminOperationLogs() throws Exception {
        mockMvc.perform(get("/api/admin/operation-logs")
                        .header("Authorization", "Bearer " + token(7L, "13800138000", AppRole.USER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    private String token(Long id, String username, AppRole role) {
        return jwtService.generateToken(new AuthPrincipal(id, username, role));
    }
}
