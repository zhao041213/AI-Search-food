package com.example.food.stats;

import com.example.food.common.GlobalExceptionHandler;
import com.example.food.stats.dto.HotIngredientItemResponse;
import com.example.food.stats.dto.HotIngredientStatsResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HotIngredientStatsController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class HotIngredientStatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HotIngredientStatsService service;

    @Test
    void returnsPublicRankingEnvelopeWithDefaultParameters() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 8, 11, 12, 0);
        when(service.get("all", 10)).thenReturn(new HotIngredientStatsResponse(
                "all",
                12,
                20,
                now,
                List.of(new HotIngredientItemResponse(1, "番茄", 8, 0.4d, now.minusMinutes(5)))
        ));

        mockMvc.perform(get("/api/stats/hot-ingredients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.period").value("all"))
                .andExpect(jsonPath("$.data.totalSearches").value(12))
                .andExpect(jsonPath("$.data.items[0].rank").value(1))
                .andExpect(jsonPath("$.data.items[0].name").value("番茄"))
                .andExpect(jsonPath("$.data.items[0].searchCount").value(8))
                .andExpect(content().string(not(containsString("userId"))))
                .andExpect(content().string(not(containsString("anonymousId"))));
    }

    @Test
    void returnsBadRequestForInvalidPeriodAndLimit() throws Exception {
        when(service.get("year", 10)).thenThrow(new IllegalArgumentException("统计周期仅支持 all、7d、30d"));
        when(service.get("all", 0)).thenThrow(new IllegalArgumentException("排行榜数量必须在 1 到 50 之间"));

        mockMvc.perform(get("/api/stats/hot-ingredients").param("period", "year"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
        mockMvc.perform(get("/api/stats/hot-ingredients").param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }
}
