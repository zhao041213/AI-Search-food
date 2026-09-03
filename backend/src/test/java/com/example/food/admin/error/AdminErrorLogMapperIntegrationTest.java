package com.example.food.admin.error;

import com.example.food.admin.error.dto.AdminErrorLogPageResponse;
import com.example.food.admin.error.dto.AdminErrorLogResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminErrorLogMapperIntegrationTest {

    @Autowired
    private AdminErrorLogMapper mapper;

    @Autowired
    private AdminErrorLogService service;

    @Test
    void listsSummaryWithoutStackTraceAndLoadsFullDetail() {
        LocalDateTime createdAt = LocalDate.now().atTime(10, 20);
        AdminErrorLog record = new AdminErrorLog();
        record.setSourceType(AdminErrorLogService.AI);
        record.setSeverity(AdminErrorLogService.ERROR);
        record.setComponent("QwenRecipeClient#generateRecipe");
        record.setExceptionClass("java.net.SocketTimeoutException");
        record.setMessage("千问服务调用失败");
        record.setRootCause("java.net.SocketTimeoutException: timeout");
        record.setRequestMethod("POST");
        record.setRequestPath("/api/ai/recipes/generate");
        record.setStatusCode(502);
        record.setIpAddress("127.0.0.1");
        record.setStackTrace("stack trace for detail");
        record.setCreatedAt(createdAt);
        mapper.insert(record);

        AdminErrorLogPageResponse page = service.list(
                AdminErrorLogService.AI,
                "Qwen",
                createdAt.toLocalDate(),
                createdAt.toLocalDate(),
                20,
                0
        );

        assertThat(page.total()).isEqualTo(1);
        assertThat(page.items()).singleElement()
                .extracting(item -> item.stackTrace())
                .isNull();

        AdminErrorLogResponse detail = service.find(record.getId());
        assertThat(detail).isNotNull();
        assertThat(detail.stackTrace()).isEqualTo("stack trace for detail");
    }
}
