package com.example.food.admin.operation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdminOperationLogInterceptorTest {

    @Test
    void mapsKnownAdminOperationsAndUsesGenericFallback() {
        assertThat(AdminOperationLogInterceptor.operationType("GET", "/api/admin/dashboard/overview"))
                .isEqualTo(AdminOperationLogService.VIEW_DASHBOARD);
        assertThat(AdminOperationLogInterceptor.operationType("GET", "/api/admin/ai-config/text-recipe"))
                .isEqualTo(AdminOperationLogService.VIEW_AI_CONFIG);
        assertThat(AdminOperationLogInterceptor.operationType("PUT", "/api/admin/ai-config/text-recipe"))
                .isEqualTo(AdminOperationLogService.UPDATE_AI_CONFIG);
        assertThat(AdminOperationLogInterceptor.operationType("DELETE", "/api/admin/anything"))
                .isEqualTo(AdminOperationLogService.ADMIN_API_ACCESS);
    }
}
