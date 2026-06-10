package com.example.food.admin;

import com.example.food.security.AppRole;
import com.example.food.security.AuthPrincipal;
import com.example.food.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AiModelConfigControllerTest {

    private static final String CONFIG_URL = "/api/admin/ai-config/text-recipe";
    private static final String SECRET = "dashscope-dashboard-secret-1234567890";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Test
    void adminCanReadAndSaveTextRecipeAiConfigWithoutApiKeyLeak() throws Exception {
        String adminToken = jwtService.generateToken(new AuthPrincipal(1L, "admin", AppRole.ADMIN));

        mockMvc.perform(get(CONFIG_URL)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.provider").value("qwen"))
                .andExpect(jsonPath("$.data.modelName").value("qwen-plus"))
                .andExpect(jsonPath("$.data.purpose").value("text_recipe"))
                .andExpect(jsonPath("$.data.apiKeyConfigured").value(false));

        mockMvc.perform(put(CONFIG_URL)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "provider": "qwen",
                                  "modelName": "qwen-max",
                                  "endpoint": "https://dashscope.example/compatible-mode/v1/chat/completions",
                                  "apiKey": "dashscope-dashboard-secret-1234567890",
                                  "enabled": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.provider").value("qwen"))
                .andExpect(jsonPath("$.data.modelName").value("qwen-max"))
                .andExpect(jsonPath("$.data.endpoint").value("https://dashscope.example/compatible-mode/v1/chat/completions"))
                .andExpect(jsonPath("$.data.apiKeyConfigured").value(true))
                .andExpect(jsonPath("$.data.apiKeyPreview").value("****7890"))
                .andExpect(content().string(not(containsString(SECRET))));

        mockMvc.perform(get(CONFIG_URL)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.modelName").value("qwen-max"))
                .andExpect(jsonPath("$.data.apiKeyConfigured").value(true))
                .andExpect(jsonPath("$.data.apiKeyPreview").value("****7890"))
                .andExpect(content().string(not(containsString(SECRET))));
    }

    @Test
    void userCannotReadAdminAiConfig() throws Exception {
        String userToken = jwtService.generateToken(new AuthPrincipal(7L, "13800138000", AppRole.USER));

        mockMvc.perform(get(CONFIG_URL)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }
}
