package com.example.food.suggestion;

import com.example.food.notification.NotificationService;
import com.example.food.suggestion.dto.FeatureSuggestionAdminUpdateRequest;
import com.example.food.suggestion.dto.FeatureSuggestionCreateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FeatureSuggestionServiceTest {
    private final FeatureSuggestionMapper suggestionMapper = mock(FeatureSuggestionMapper.class);
    private final FeatureSuggestionAdditionMapper additionMapper = mock(FeatureSuggestionAdditionMapper.class);
    private final FeatureSuggestionAuditLogMapper auditLogMapper = mock(FeatureSuggestionAuditLogMapper.class);
    private final FeatureSuggestionFileStorage fileStorage = mock(FeatureSuggestionFileStorage.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final FeatureSuggestionService service = new FeatureSuggestionService(
            suggestionMapper, additionMapper, auditLogMapper, fileStorage, notificationService
    );

    @BeforeEach
    void setUp() {
        when(additionMapper.findBySuggestionId(any())).thenReturn(List.of());
    }

    @Test
    void createStoresPendingSuggestionWithoutExposingInternalFields() {
        doAnswer(invocation -> {
            FeatureSuggestion item = invocation.getArgument(0);
            item.setId(12L);
            return 1;
        }).when(suggestionMapper).insert(any(FeatureSuggestion.class));

        var response = service.create(7L,
                new FeatureSuggestionCreateRequest("UX_IMPROVEMENT", "改进搜索结果", "希望可以按烹饪时间筛选菜谱", "更快找到合适菜谱"),
                null);

        assertThat(response.id()).isEqualTo(12L);
        assertThat(response.status()).isEqualTo("PENDING");
        verify(auditLogMapper).insert(any(FeatureSuggestionAuditLog.class));
    }

    @Test
    void rateLimitStopsRepeatedSubmissions() {
        when(suggestionMapper.countRecent(eq(7L), any())).thenReturn(5L);

        assertThatThrownBy(() -> service.create(7L,
                new FeatureSuggestionCreateRequest("OTHER", "重复提交", "这是用于测试频率限制的足够长描述", null), null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("每小时最多提交");
    }

    @Test
    void adminProcessingNotifiesSuggestionOwner() {
        FeatureSuggestion suggestion = suggestion();
        when(suggestionMapper.findActive(12L)).thenReturn(suggestion);
        when(suggestionMapper.updateAdmin(any(FeatureSuggestion.class))).thenReturn(1);

        var response = service.updateAdmin(99L, 12L,
                new FeatureSuggestionAdminUpdateRequest("COMPLETED", "已经完成，谢谢反馈", "已发布", null));

        assertThat(response.status()).isEqualTo("COMPLETED");
        verify(notificationService).notifyUser(eq(7L), eq(com.example.food.notification.NotificationType.FEATURE_SUGGESTION),
                any(), any(), any(), eq("/feature-suggestions"), any());
        verify(auditLogMapper).insert(any(FeatureSuggestionAuditLog.class));
    }

    private FeatureSuggestion suggestion() {
        FeatureSuggestion suggestion = new FeatureSuggestion();
        suggestion.setId(12L);
        suggestion.setUserId(7L);
        suggestion.setType("BUG_REPORT");
        suggestion.setTitle("搜索结果异常");
        suggestion.setDetail("这是用于测试管理员处理流程的足够长描述");
        suggestion.setStatus("PENDING");
        return suggestion;
    }
}
