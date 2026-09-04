package com.example.food.user.security;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserSecurityEventService {
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILURE = "FAILURE";
    public static final String PROFILE_UPDATED = "PROFILE_UPDATED";
    public static final String AVATAR_UPDATED = "AVATAR_UPDATED";
    public static final String LOGOUT_ALL_DEVICES = "LOGOUT_ALL_DEVICES";
    public static final String PASSWORD_RESET_SESSION_INVALIDATED = "PASSWORD_RESET_SESSION_INVALIDATED";
    public static final String ACCOUNT_CANCELLED = "ACCOUNT_CANCELLED";

    private final UserSecurityEventMapper mapper;

    public UserSecurityEventService(UserSecurityEventMapper mapper) {
        this.mapper = mapper;
    }

    public void record(Long userId, String eventType, String result, String ipAddress, String details) {
        UserSecurityEvent event = new UserSecurityEvent();
        event.setUserId(userId);
        event.setEventType(limit(eventType, 64));
        event.setEventResult(SUCCESS.equals(result) ? SUCCESS : FAILURE);
        event.setIpAddress(limit(ipAddress, 64));
        event.setDetails(limit(details, 255));
        event.setCreatedAt(LocalDateTime.now());
        mapper.insert(event);
    }

    private String limit(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
