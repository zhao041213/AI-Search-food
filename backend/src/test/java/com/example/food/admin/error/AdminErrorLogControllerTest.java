package com.example.food.admin.error;

import com.example.food.admin.error.dto.AdminErrorLogPageResponse;
import com.example.food.admin.error.dto.AdminErrorLogResponse;
import com.example.food.common.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminErrorLogController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AdminErrorLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminErrorLogService service;

    @Test
    void listsFilteredErrorSummaries() throws Exception {
        AdminErrorLogResponse item = response(12L, null);
        when(service.list(eq("AI"), eq("Qwen"), eq(null), eq(null), eq(20), eq(0)))
                .thenReturn(new AdminErrorLogPageResponse(List.of(item), 1, 20, 0));

        mockMvc.perform(get("/api/admin/error-logs")
                        .param("sourceType", "AI")
                        .param("keyword", "Qwen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].sourceType").value("AI"))
                .andExpect(jsonPath("$.data.items[0].stackTrace").doesNotExist());
    }

    @Test
    void returnsErrorDetailById() throws Exception {
        when(service.find(12L)).thenReturn(response(12L, "at QwenRecipeClient.generateRecipe"));

        mockMvc.perform(get("/api/admin/error-logs/12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(12))
                .andExpect(jsonPath("$.data.stackTrace").value("at QwenRecipeClient.generateRecipe"));
    }

    private AdminErrorLogResponse response(Long id, String stackTrace) {
        return new AdminErrorLogResponse(
                id,
                AdminErrorLogService.AI,
                AdminErrorLogService.ERROR,
                "QwenRecipeClient#generateRecipe",
                "org.example.AiException",
                "千问服务调用失败",
                "java.net.SocketTimeoutException: timeout",
                "POST",
                "/api/ai/recipes/generate",
                502,
                7L,
                null,
                "10.0.0.8",
                stackTrace,
                LocalDateTime.of(2026, 9, 3, 10, 20)
        );
    }
}
