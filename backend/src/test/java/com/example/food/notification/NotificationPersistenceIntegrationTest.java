package com.example.food.notification;

import com.example.food.notification.dto.NotificationPreferenceRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotificationPersistenceIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private NotificationService service;

    private Long firstUserId;
    private Long secondUserId;

    @BeforeEach
    void setUp() {
        firstUserId = insertUser("notification-owner");
        secondUserId = insertUser("notification-other");
        insertNotification(firstUserId, "PANTRY_EXPIRING", "第一用户消息", "PANTRY|PANTRY_EXPIRING|番茄|2026-09-06", "UNREAD");
        insertNotification(secondUserId, "PANTRY_EXPIRING", "第二用户消息", "PANTRY|PANTRY_EXPIRING|番茄|2026-09-06", "UNREAD");
    }

    @Test
    void isolatesUsersAndPersistsReadArchiveAndPaginationState() {
        var page = service.list(firstUserId, "all", 1, 1);

        assertThat(page.total()).isEqualTo(1);
        assertThat(page.items()).singleElement().extracting("title").isEqualTo("第一用户消息");
        assertThat(service.unreadCount(firstUserId)).isEqualTo(1);
        assertThat(service.unreadCount(secondUserId)).isEqualTo(1);

        service.markRead(firstUserId, page.items().get(0).id());
        assertThat(service.unreadCount(firstUserId)).isZero();
        assertThat(service.list(firstUserId, "read", 1, 10).total()).isEqualTo(1);

        service.archive(firstUserId, page.items().get(0).id());
        assertThat(service.list(firstUserId, "archived", 1, 10).total()).isEqualTo(1);
        assertThat(service.list(secondUserId, "unread", 1, 10).total()).isEqualTo(1);
    }

    @Test
    void notificationPreferencesDefaultToEnabledAndCanBeUpdated() {
        assertThat(service.getPreferences(firstUserId).pantryExpiringEnabled()).isTrue();
        service.updatePreferences(firstUserId, new NotificationPreferenceRequest(false, true, false));

        var updated = service.getPreferences(firstUserId);

        assertThat(updated.pantryExpiringEnabled()).isFalse();
        assertThat(updated.pantryExpiredEnabled()).isTrue();
        assertThat(updated.weeklyMenuPreparationEnabled()).isFalse();
    }

    private Long insertUser(String nickname) {
        String phone = "139" + String.format("%08d", Math.abs(System.nanoTime() % 100_000_000));
        jdbcTemplate.update("INSERT INTO users (phone, nickname) VALUES (?, ?)", phone, nickname);
        return jdbcTemplate.queryForObject("SELECT id FROM users WHERE phone = ?", Long.class, phone);
    }

    private void insertNotification(Long userId, String type, String title, String dedupeKey, String status) {
        jdbcTemplate.update("""
                INSERT INTO user_notifications (
                    user_id, notification_type, title, summary, content, status, dedupe_key
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """, userId, type, title, "摘要", "详情", status, dedupeKey);
    }
}
