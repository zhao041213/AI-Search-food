package com.example.food.recipe;

import com.example.food.recipe.dto.RecommendationFeedbackResponse;
import com.example.food.recipe.dto.RecommendationReactionRequest;
import com.example.food.security.AppRole;
import com.example.food.security.AuthPrincipal;
import com.example.food.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecommendationFeedbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RecommendationFeedbackService service;

    @Test
    void feedbackReadRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/recommendation-feedbacks/42"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void userCanReadEmptyFeedbackState() throws Exception {
        when(service.get(eq(42L), any(AuthPrincipal.class), eq("browser-123")))
                .thenReturn(RecommendationFeedbackResponse.empty(42L));
        String token = token(7L, AppRole.USER);

        mockMvc.perform(get("/api/recommendation-feedbacks/42")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Anonymous-Id", "browser-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.searchLogId").value(42))
                .andExpect(jsonPath("$.data.cooked").value(false))
                .andExpect(jsonPath("$.data.reaction").doesNotExist());
    }

    @Test
    void userCanSetAndClearReaction() throws Exception {
        String token = token(7L, AppRole.USER);
        when(service.setReaction(eq(42L), any(RecommendationReactionRequest.class), any(AuthPrincipal.class), eq(null)))
                .thenReturn(new RecommendationFeedbackResponse(42L, "LIKE", false, null, null));
        when(service.clearReaction(eq(42L), any(AuthPrincipal.class), eq(null)))
                .thenReturn(RecommendationFeedbackResponse.empty(42L));

        mockMvc.perform(put("/api/recommendation-feedbacks/42/reaction")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RecommendationReactionRequest("LIKE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reaction").value("LIKE"));
        mockMvc.perform(delete("/api/recommendation-feedbacks/42/reaction")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reaction").doesNotExist());

        verify(service).setReaction(eq(42L), any(RecommendationReactionRequest.class), any(AuthPrincipal.class), eq(null));
        verify(service).clearReaction(eq(42L), any(AuthPrincipal.class), eq(null));
    }

    @Test
    void adminCannotUseUserFeedbackApi() throws Exception {
        mockMvc.perform(put("/api/recommendation-feedbacks/42/cooked")
                        .header("Authorization", "Bearer " + token(1L, AppRole.ADMIN)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void reactionValidationRejectsBlankValue() throws Exception {
        mockMvc.perform(put("/api/recommendation-feedbacks/42/reaction")
                        .header("Authorization", "Bearer " + token(7L, AppRole.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reaction\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    private String token(Long userId, AppRole role) {
        return jwtService.generateToken(new AuthPrincipal(userId, role == AppRole.ADMIN ? "admin" : "13800138000", role));
    }
}
