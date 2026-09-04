package com.example.food.notification;

import com.example.food.notification.dto.NotificationPageResponse;
import com.example.food.notification.dto.NotificationPreferenceRequest;
import com.example.food.notification.dto.NotificationPreferenceResponse;
import com.example.food.notification.dto.NotificationResponse;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

@Service
public class NotificationService {

    public static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 50;

    private final UserNotificationMapper notificationMapper;
    private final UserNotificationPreferenceMapper preferenceMapper;
    private final Clock clock;

    @Autowired
    public NotificationService(
            UserNotificationMapper notificationMapper,
            UserNotificationPreferenceMapper preferenceMapper
    ) {
        this(notificationMapper, preferenceMapper, Clock.system(ZONE));
    }

    NotificationService(
            UserNotificationMapper notificationMapper,
            UserNotificationPreferenceMapper preferenceMapper,
            Clock clock
    ) {
        this.notificationMapper = notificationMapper;
        this.preferenceMapper = preferenceMapper;
        this.clock = clock;
    }

    public NotificationPageResponse list(Long userId, String status, int page, int size) {
        validatePage(page, size);
        String statusValue = statusValue(status);
        int offset = Math.toIntExact((long) (page - 1) * size);
        List<NotificationResponse> items = notificationMapper.findPage(userId, statusValue, size, offset)
                .stream()
                .map(this::toResponse)
                .toList();
        long total = notificationMapper.countByUserIdAndStatus(userId, statusValue);
        int totalPages = total == 0 ? 0 : (int) Math.ceil(total / (double) size);
        return new NotificationPageResponse(items, total, page, size, totalPages);
    }

    public long unreadCount(Long userId) {
        return notificationMapper.countUnread(userId);
    }

    public NotificationResponse detail(Long userId, Long notificationId) {
        UserNotification notification = requireOwned(userId, notificationId);
        if (NotificationStatus.UNREAD.name().equals(notification.getStatus())) {
            notificationMapper.markRead(userId, notificationId);
            notification = notificationMapper.findOwned(userId, notificationId);
        }
        return toResponse(notification);
    }

    public NotificationResponse markRead(Long userId, Long notificationId) {
        requireOwned(userId, notificationId);
        notificationMapper.markRead(userId, notificationId);
        return toResponse(requireOwned(userId, notificationId));
    }

    public long markAllRead(Long userId) {
        return notificationMapper.markAllRead(userId);
    }

    public NotificationResponse archive(Long userId, Long notificationId) {
        requireOwned(userId, notificationId);
        notificationMapper.archive(userId, notificationId);
        return toResponse(requireOwned(userId, notificationId));
    }

    public NotificationPreferenceResponse getPreferences(Long userId) {
        return toPreferenceResponse(preferenceMapper.findByUserId(userId));
    }

    @Transactional
    public NotificationPreferenceResponse updatePreferences(
            Long userId,
            NotificationPreferenceRequest request
    ) {
        LocalDateTime now = LocalDateTime.now(clock);
        UserNotificationPreference preference = preferenceMapper.findByUserId(userId);
        if (preference == null) {
            preference = new UserNotificationPreference();
            preference.setUserId(userId);
            preference.setCreatedAt(now);
            applyPreference(preference, request, now);
            try {
                preferenceMapper.insert(preference);
            } catch (DuplicateKeyException exception) {
                preference = preferenceMapper.findByUserId(userId);
                if (preference == null) {
                    throw exception;
                }
                applyPreference(preference, request, now);
                preferenceMapper.updateByUserId(preference);
            }
        } else {
            applyPreference(preference, request, now);
            preferenceMapper.updateByUserId(preference);
        }
        return toPreferenceResponse(preference);
    }

    private String statusValue(String status) {
        if (!StringUtils.hasText(status) || "ALL".equalsIgnoreCase(status.trim())) {
            return null;
        }
        try {
            return NotificationStatus.valueOf(status.trim().toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("消息状态不合法");
        }
    }

    private void validatePage(int page, int size) {
        if (page < 1) {
            throw new IllegalArgumentException("页码必须从 1 开始");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("每页数量必须在 1 到 " + MAX_PAGE_SIZE + " 之间");
        }
        if ((long) (page - 1) * size > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("页码超出范围");
        }
    }

    private UserNotification requireOwned(Long userId, Long notificationId) {
        UserNotification notification = notificationMapper.findOwned(userId, notificationId);
        if (notification == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "消息不存在");
        }
        return notification;
    }

    private void applyPreference(
            UserNotificationPreference preference,
            NotificationPreferenceRequest request,
            LocalDateTime now
    ) {
        preference.setPantryExpiringEnabled(request.pantryExpiringEnabled());
        preference.setPantryExpiredEnabled(request.pantryExpiredEnabled());
        preference.setWeeklyMenuPreparationEnabled(request.weeklyMenuPreparationEnabled());
        preference.setUpdatedAt(now);
    }

    private NotificationResponse toResponse(UserNotification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getNotificationType(),
                notification.getTitle(),
                notification.getSummary(),
                notification.getContent(),
                notification.getTargetPath(),
                notification.getStatus(),
                notification.getCreatedAt(),
                notification.getReadAt(),
                notification.getArchivedAt()
        );
    }

    private NotificationPreferenceResponse toPreferenceResponse(UserNotificationPreference preference) {
        if (preference == null) {
            return new NotificationPreferenceResponse(true, true, true);
        }
        return new NotificationPreferenceResponse(
                !Boolean.FALSE.equals(preference.getPantryExpiringEnabled()),
                !Boolean.FALSE.equals(preference.getPantryExpiredEnabled()),
                !Boolean.FALSE.equals(preference.getWeeklyMenuPreparationEnabled())
        );
    }
}
