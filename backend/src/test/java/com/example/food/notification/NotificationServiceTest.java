package com.example.food.notification;

import com.example.food.notification.dto.NotificationPreferenceRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private UserNotificationMapper notificationMapper;

    @Mock
    private UserNotificationPreferenceMapper preferenceMapper;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                Instant.parse("2026-09-04T06:00:00Z"),
                ZoneId.of("Asia/Shanghai")
        );
        service = new NotificationService(notificationMapper, preferenceMapper, clock);
    }

    @Test
    void listsOnlyRequestedUserAndStatusPage() {
        UserNotification item = notification(21L, 7L, NotificationStatus.READ.name());
        when(notificationMapper.findPage(7L, NotificationStatus.READ.name(), 10, 10))
                .thenReturn(List.of(item));
        when(notificationMapper.countByUserIdAndStatus(7L, NotificationStatus.READ.name()))
                .thenReturn(11L);

        var page = service.list(7L, "read", 2, 10);

        assertThat(page.items()).singleElement().extracting("id").isEqualTo(21L);
        assertThat(page.total()).isEqualTo(11L);
        assertThat(page.totalPages()).isEqualTo(2);
        verify(notificationMapper).findPage(7L, NotificationStatus.READ.name(), 10, 10);
    }

    @Test
    void allStatusDoesNotAddStatusPredicate() {
        when(notificationMapper.findPage(7L, null, 10, 0)).thenReturn(List.of());
        when(notificationMapper.countByUserIdAndStatus(7L, null)).thenReturn(0L);

        var page = service.list(7L, "all", 1, 10);

        assertThat(page.items()).isEmpty();
        verify(notificationMapper).findPage(7L, null, 10, 0);
    }

    @Test
    void rejectsInvalidStatusAndPageArguments() {
        assertThatThrownBy(() -> service.list(7L, "deleted", 1, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("消息状态不合法");
        assertThatThrownBy(() -> service.list(7L, "all", 0, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("页码必须从 1 开始");
        assertThatThrownBy(() -> service.list(7L, "all", 1, 51))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void detailMarksOnlyOwnedUnreadMessageAsRead() {
        UserNotification unread = notification(3L, 7L, NotificationStatus.UNREAD.name());
        UserNotification read = notification(3L, 7L, NotificationStatus.READ.name());
        when(notificationMapper.findOwned(7L, 3L)).thenReturn(unread, read);

        var response = service.detail(7L, 3L);

        assertThat(response.status()).isEqualTo(NotificationStatus.READ.name());
        verify(notificationMapper).markRead(7L, 3L);
    }

    @Test
    void doesNotExposeAnotherUsersMessage() {
        when(notificationMapper.findOwned(eq(7L), eq(3L))).thenReturn(null);

        assertThatThrownBy(() -> service.archive(7L, 3L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("消息不存在");
        verify(notificationMapper, never()).archive(7L, 3L);
    }

    @Test
    void defaultsPreferencesToEnabledAndPersistsUserOnlyChanges() {
        when(preferenceMapper.findByUserId(7L)).thenReturn(null);
        var request = new NotificationPreferenceRequest(false, true, false);

        var response = service.updatePreferences(7L, request);

        assertThat(response.pantryExpiringEnabled()).isFalse();
        assertThat(response.pantryExpiredEnabled()).isTrue();
        assertThat(response.weeklyMenuPreparationEnabled()).isFalse();
        org.mockito.ArgumentCaptor<UserNotificationPreference> captor =
                org.mockito.ArgumentCaptor.forClass(UserNotificationPreference.class);
        verify(preferenceMapper).insert(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(7L);
        assertThat(captor.getValue().getPantryExpiringEnabled()).isFalse();
        assertThat(captor.getValue().getPantryExpiredEnabled()).isTrue();
        assertThat(captor.getValue().getWeeklyMenuPreparationEnabled()).isFalse();
    }

    private UserNotification notification(Long id, Long userId, String status) {
        UserNotification notification = new UserNotification();
        notification.setId(id);
        notification.setUserId(userId);
        notification.setNotificationType(NotificationType.PANTRY_EXPIRING.name());
        notification.setTitle("库存提醒");
        notification.setSummary("摘要");
        notification.setContent("详情");
        notification.setStatus(status);
        notification.setCreatedAt(LocalDateTime.of(2026, 9, 4, 14, 0));
        return notification;
    }
}
