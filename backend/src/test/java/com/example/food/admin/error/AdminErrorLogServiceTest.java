package com.example.food.admin.error;

import com.example.food.admin.error.dto.AdminErrorLogPageResponse;
import com.example.food.security.AppRole;
import com.example.food.security.AuthPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminErrorLogServiceTest {

    @Mock
    private AdminErrorLogMapper mapper;

    private AdminErrorLogService service;

    @BeforeEach
    void setUp() {
        service = new AdminErrorLogService(mapper);
    }

    @Test
    void recordsAiFailureWithActorContextAndRedactsSecrets() {
        RuntimeException exception = new RuntimeException("千问服务调用失败 apiKey=secret-value");

        service.recordException(
                exception,
                new AuthPrincipal(7L, "cook-admin", AppRole.ADMIN),
                "POST",
                "/api/ai/recipes/generate",
                502,
                "10.0.0.8"
        );

        ArgumentCaptor<AdminErrorLog> captor = ArgumentCaptor.forClass(AdminErrorLog.class);
        verify(mapper).insert(captor.capture());
        AdminErrorLog record = captor.getValue();
        assertThat(record.getSourceType()).isEqualTo(AdminErrorLogService.AI);
        assertThat(record.getAdminId()).isEqualTo(7L);
        assertThat(record.getRequestPath()).isEqualTo("/api/ai/recipes/generate");
        assertThat(record.getStatusCode()).isEqualTo(502);
        assertThat(record.getMessage()).doesNotContain("secret-value");
        assertThat(record.getStackTrace()).doesNotContain("secret-value");
        assertThat(record.getCreatedAt()).isNotNull();
    }

    @Test
    void classifiesDatabaseFailureAndNormalizesDatePagingArguments() {
        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDateTime to = LocalDate.of(2026, 9, 3).plusDays(1).atStartOfDay();
        when(mapper.findPage(
                AdminErrorLogService.DATABASE,
                "连接",
                from.atStartOfDay(),
                to,
                100,
                12
        )).thenReturn(List.of());
        when(mapper.count(AdminErrorLogService.DATABASE, "连接", from.atStartOfDay(), to)).thenReturn(0L);

        service.recordException(
                new DataAccessResourceFailureException("数据库连接失败"),
                null,
                "GET",
                "/api/user/pantry",
                500,
                null
        );
        AdminErrorLogPageResponse page = service.list(
                "database",
                " 连接 ",
                from,
                LocalDate.of(2026, 9, 3),
                500,
                12
        );

        assertThat(page.total()).isZero();
        assertThat(page.limit()).isEqualTo(100);
        verify(mapper).findPage(
                AdminErrorLogService.DATABASE,
                "连接",
                from.atStartOfDay(),
                to,
                100,
                12
        );
    }

    @Test
    void rejectsInvalidSourceAndDateRange() {
        assertThatThrownBy(() -> service.list("CACHE", null, null, null, 20, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("异常来源不合法");
        assertThatThrownBy(() -> service.list(
                AdminErrorLogService.AI,
                null,
                LocalDate.of(2026, 9, 3),
                LocalDate.of(2026, 9, 1),
                20,
                0
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("异常时间范围不合法");
    }

    @Test
    void errorLogPersistenceFailureDoesNotEscapeOriginalFailurePath() {
        when(mapper.insert(any(AdminErrorLog.class))).thenThrow(new IllegalStateException("database unavailable"));

        service.recordFailure(
                AdminErrorLogService.TOOL,
                "WikimediaIngredientImageProvider",
                "Tool 调用失败",
                null,
                null,
                "ASYNC",
                "/internal/tools/ingredient-image-cache",
                502,
                null
        );
    }
}
