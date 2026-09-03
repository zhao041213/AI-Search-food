package com.example.food.admin.operation;

import com.example.food.admin.operation.dto.AdminOperationLogPageResponse;
import com.example.food.admin.operation.dto.AdminOperationLogResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminOperationLogService {

    public static final String SUCCESS = "SUCCESS";
    public static final String FAILURE = "FAILURE";
    public static final String ADMIN_LOGIN = "ADMIN_LOGIN";
    public static final String VIEW_DASHBOARD = "VIEW_DASHBOARD";
    public static final String VIEW_AI_CONFIG = "VIEW_AI_CONFIG";
    public static final String UPDATE_AI_CONFIG = "UPDATE_AI_CONFIG";
    public static final String ADMIN_API_ACCESS = "ADMIN_API_ACCESS";

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;
    private static final Logger log = LoggerFactory.getLogger(AdminOperationLogService.class);

    private final AdminOperationLogMapper mapper;

    public AdminOperationLogService(AdminOperationLogMapper mapper) {
        this.mapper = mapper;
    }

    public void record(
            Long adminId,
            String adminUsername,
            String operationType,
            String httpMethod,
            String requestPath,
            String operationResult,
            int statusCode,
            String ipAddress,
            String details
    ) {
        AdminOperationLog record = new AdminOperationLog();
        record.setAdminId(adminId);
        record.setAdminUsername(limitText(valueOrFallback(adminUsername, "unknown"), 64));
        record.setOperationType(limitText(valueOrFallback(operationType, ADMIN_API_ACCESS), 64));
        record.setHttpMethod(limitText(valueOrFallback(httpMethod, "UNKNOWN"), 16));
        record.setRequestPath(limitText(valueOrFallback(requestPath, "unknown"), 255));
        record.setOperationResult(SUCCESS.equals(operationResult) ? SUCCESS : FAILURE);
        record.setStatusCode(statusCode);
        record.setIpAddress(limitText(ipAddress, 64));
        record.setDetails(limitText(details, 255));
        record.setCreatedAt(LocalDateTime.now());
        try {
            mapper.insert(record);
        } catch (RuntimeException exception) {
            // Auditing must not turn a successful login or admin action into a 500 response.
            log.warn("Unable to persist admin operation log for {} {}", record.getHttpMethod(), record.getRequestPath(), exception);
        }
    }

    public AdminOperationLogPageResponse list(String keyword, String result, int limit, int offset) {
        String normalizedKeyword = normalizeKeyword(keyword);
        String normalizedResult = normalizeResult(result);
        int safeLimit = limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        int safeOffset = Math.max(0, offset);
        List<AdminOperationLogResponse> items = mapper.findPage(
                        normalizedKeyword,
                        normalizedResult,
                        safeLimit,
                        safeOffset
                ).stream()
                .map(this::responseOf)
                .toList();
        return new AdminOperationLogPageResponse(
                items,
                mapper.count(normalizedKeyword, normalizedResult),
                safeLimit,
                safeOffset
        );
    }

    private AdminOperationLogResponse responseOf(AdminOperationLog record) {
        return new AdminOperationLogResponse(
                record.getId(),
                record.getAdminId(),
                record.getAdminUsername(),
                record.getOperationType(),
                record.getHttpMethod(),
                record.getRequestPath(),
                record.getOperationResult(),
                record.getStatusCode(),
                record.getIpAddress(),
                record.getDetails(),
                record.getCreatedAt()
        );
    }

    private String normalizeKeyword(String keyword) {
        return StringUtils.hasText(keyword) ? limitText(keyword.trim(), 64) : null;
    }

    private String normalizeResult(String result) {
        if (!StringUtils.hasText(result)) {
            return null;
        }
        if (!SUCCESS.equals(result) && !FAILURE.equals(result)) {
            throw new IllegalArgumentException("Invalid operation result");
        }
        return result;
    }

    private String valueOrFallback(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String limitText(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}
