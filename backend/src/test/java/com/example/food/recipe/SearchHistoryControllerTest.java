package com.example.food.recipe;

import com.example.food.recipe.dto.RecentSearchResponse;
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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SearchHistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private SearchHistoryService searchHistoryService;

    @Test
    void userCanReadOwnRecentSearches() throws Exception {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 13, 20, 30);
        when(searchHistoryService.recent(7L)).thenReturn(List.of(
                new RecentSearchResponse(
                        9L,
                        "番茄、鸡蛋",
                        "dinner",
                        "balanced",
                        "text",
                        createdAt
                )
        ));
        String token = jwtService.generateToken(new AuthPrincipal(7L, "13800138000", AppRole.USER));

        mockMvc.perform(get("/api/search-history/recent")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].id").value(9))
                .andExpect(jsonPath("$.data[0].ingredients").value("番茄、鸡蛋"))
                .andExpect(jsonPath("$.data[0].mealType").value("dinner"))
                .andExpect(jsonPath("$.data[0].goal").value("balanced"))
                .andExpect(jsonPath("$.data[0].searchMode").value("text"))
                .andExpect(jsonPath("$.data[0].createdAt").value("2026-08-13T20:30:00"));

        verify(searchHistoryService).recent(7L);
    }

    @Test
    void recentSearchesRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/search-history/recent"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void adminCannotReadUserRecentSearches() throws Exception {
        String token = jwtService.generateToken(new AuthPrincipal(1L, "admin", AppRole.ADMIN));

        mockMvc.perform(get("/api/search-history/recent")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }
}
