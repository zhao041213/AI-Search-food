package com.example.food.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class UserSecurityLogService {

    public static final String COLLECTION_CHANGE = "COLLECTION_CHANGE";
    public static final String BATCH_SAVED_RECIPE_CHANGE = "BATCH_SAVED_RECIPE_CHANGE";
    public static final String SHARE_CREATE = "SHARE_CREATE";
    public static final String SHARE_DISABLE = "SHARE_DISABLE";

    private static final Logger log = LoggerFactory.getLogger(UserSecurityLogService.class);
    private final UserSecurityLogMapper mapper;

    public UserSecurityLogService(UserSecurityLogMapper mapper) {
        this.mapper = mapper;
    }

    public void record(Long userId, String actionType, String requestPath, String details) {
        if (userId == null) {
            return;
        }
        UserSecurityLog record = new UserSecurityLog();
        record.setUserId(userId);
        record.setActionType(limit(actionType, 64));
        record.setRequestPath(limit(requestPath, 255));
        record.setDetails(limit(details, 1000));
        record.setCreatedAt(LocalDateTime.now());
        try {
            mapper.insert(record);
        } catch (RuntimeException exception) {
            log.warn("Unable to persist user security log for {}", actionType, exception);
        }
    }

    private String limit(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return "unknown";
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}
