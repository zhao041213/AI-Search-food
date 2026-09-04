package com.example.food.admin.operation;

import com.example.food.admin.error.AdminErrorLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminOperationLogServiceTest {

    @Mock
    private AdminOperationLogMapper mapper;

    @Mock
    private AdminErrorLogService errorLogService;

    private AdminOperationLogService service;

    @BeforeEach
    void setUp() {
        service = new AdminOperationLogService(mapper, errorLogService);
    }

    @Test
    void recordsAdminOperationWithoutSensitiveFields() {
        service.record(
                7L,
                "admin",
                AdminOperationLogService.UPDATE_AI_CONFIG,
                "PUT",
                "/api/admin/ai-config/text-recipe",
                AdminOperationLogService.SUCCESS,
                200,
                "10.0.0.1",
                "配置已保存"
        );

        ArgumentCaptor<AdminOperationLog> captor = ArgumentCaptor.forClass(AdminOperationLog.class);
        verify(mapper).insert(captor.capture());
        AdminOperationLog record = captor.getValue();
        assertThat(record.getAdminId()).isEqualTo(7L);
        assertThat(record.getAdminUsername()).isEqualTo("admin");
        assertThat(record.getOperationType()).isEqualTo(AdminOperationLogService.UPDATE_AI_CONFIG);
        assertThat(record.getOperationResult()).isEqualTo(AdminOperationLogService.SUCCESS);
        assertThat(record.getStatusCode()).isEqualTo(200);
        assertThat(record.getDetails()).doesNotContain("password", "apiKey", "secret");
        assertThat(record.getCreatedAt()).isNotNull();
    }

    @Test
    void normalizesPageArgumentsAndRejectsUnknownResult() {
        when(mapper.findPage(null, AdminOperationLogService.SUCCESS, 100, 12)).thenReturn(List.of());
        when(mapper.count(null, AdminOperationLogService.SUCCESS)).thenReturn(0L);

        AdminOperationLogService service = this.service;
        assertThat(service.list(" ", "SUCCESS", 500, 12).limit()).isEqualTo(100);
        verify(mapper).findPage(null, AdminOperationLogService.SUCCESS, 100, 12);

        assertThatThrownBy(() -> service.list(null, "UNKNOWN", 20, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid operation result");
    }

    @Test
    void auditPersistenceFailureDoesNotBreakAdminAction() {
        when(mapper.insert(any(AdminOperationLog.class))).thenThrow(new IllegalStateException("database unavailable"));

        service.record(
                7L,
                "admin",
                AdminOperationLogService.VIEW_DASHBOARD,
                "GET",
                "/api/admin/dashboard/overview",
                AdminOperationLogService.SUCCESS,
                200,
                "127.0.0.1",
                null
        );

        verify(errorLogService).recordFailure(
                org.mockito.ArgumentMatchers.eq(AdminErrorLogService.DATABASE),
                org.mockito.ArgumentMatchers.eq("AdminOperationLogService.record"),
                org.mockito.ArgumentMatchers.eq("管理员操作日志写入失败"),
                any(RuntimeException.class),
                any(com.example.food.security.AuthPrincipal.class),
                org.mockito.ArgumentMatchers.eq("GET"),
                org.mockito.ArgumentMatchers.eq("/api/admin/dashboard/overview"),
                org.mockito.ArgumentMatchers.eq(200),
                org.mockito.ArgumentMatchers.eq("127.0.0.1")
        );
    }
}
