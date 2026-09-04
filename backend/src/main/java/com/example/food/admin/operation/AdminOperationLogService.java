package com.example.food.admin.operation;

import com.example.food.admin.error.AdminErrorLogService;
import com.example.food.admin.operation.dto.AdminOperationLogPageResponse;
import com.example.food.admin.operation.dto.AdminOperationLogResponse;
import com.example.food.security.AppRole;
import com.example.food.security.AuthPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
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
    public static final String VIEW_USERS = "VIEW_USERS";
    public static final String USER_DISABLE = "USER_DISABLE";
    public static final String USER_ENABLE = "USER_ENABLE";
    public static final String USER_PASSWORD_UNLOCK = "USER_PASSWORD_UNLOCK";
    public static final String ADMIN_API_ACCESS = "ADMIN_API_ACCESS";

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;
    private static final Logger log = LoggerFactory.getLogger(AdminOperationLogService.class);

    private final AdminOperationLogMapper mapper;
    private final AdminErrorLogService errorLogService;

    @Autowired
    public AdminOperationLogService(AdminOperationLogMapper mapper, AdminErrorLogService errorLogService) {
        this.mapper = mapper;
        this.errorLogService = errorLogService;
    }

    public AdminOperationLogService(AdminOperationLogMapper mapper) {
        this(mapper, null);
    }

    public void recordUserStatusChange(
            AuthPrincipal principal,
            Long userId,
            String maskedPhone,
            boolean enabled,
            String reason,
            boolean changed,
            String result,
            int statusCode,
            String ipAddress
    ) {
        record(
                principal == null ? null : principal.id(),
                principal == null ? null : principal.username(),
                enabled ? USER_ENABLE : USER_DISABLE,
                "PUT",
                "/api/admin/users/" + userId + "/status",
                result,
                statusCode,
                ipAddress,
                userDetails(userId, maskedPhone, reason, changed)
        );
    }

    public void recordPasswordUnlock(
            AuthPrincipal principal,
            Long userId,
            String maskedPhone,
            boolean changed,
            String result,
            int statusCode,
            String ipAddress
    ) {
        record(
                principal == null ? null : principal.id(),
                principal == null ? null : principal.username(),
                USER_PASSWORD_UNLOCK,
                "POST",
                "/api/admin/users/" + userId + "/password-lock/clear",
                result,
                statusCode,
                ipAddress,
                userDetails(userId, maskedPhone, null, changed)
        );
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
        record.setDetails(limitText(sanitizeDetails(details), 255));
        record.setCreatedAt(LocalDateTime.now());
        try {
            mapper.insert(record);
        } catch (RuntimeException exception) {
            // Auditing must not turn a successful login or admin action into a 500 response.
            log.warn("Unable to persist admin operation log for {} {}", record.getHttpMethod(), record.getRequestPath(), exception);
            if (errorLogService != null) {
                try {
                    errorLogService.recordFailure(
                            AdminErrorLogService.DATABASE,
                            "AdminOperationLogService.record",
                            "管理员操作日志写入失败",
                            exception,
                            adminPrincipal(record),
                            record.getHttpMethod(),
                            record.getRequestPath(),
                            record.getStatusCode(),
                            record.getIpAddress()
                    );
                } catch (RuntimeException loggingException) {
                    log.warn("Unable to persist the admin operation log failure", loggingException);
                }
            }
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

    private String userDetails(Long userId, String maskedPhone, String reason, boolean changed) {
        String normalizedReason = limitText(reason, 255);
        return limitText(
                "targetUserId=" + userId
                        + "; phone=" + valueOrFallback(maskedPhone, "unknown")
                        + "; reason=" + valueOrFallback(normalizedReason, "none")
                        + "; changed=" + changed,
                255
        );
    }

    private AuthPrincipal adminPrincipal(AdminOperationLog record) {
        if (record.getAdminId() == null) {
            return null;
        }
        return new AuthPrincipal(record.getAdminId(), record.getAdminUsername(), AppRole.ADMIN);
    }

    private String limitText(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private String sanitizeDetails(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll(
                "(?i)((?:password(?:_hash)?|password|token|jwt|authorization|api[-_ ]?key|secret|密码|验证码)\\s*[:=]\\s*)[^;\\s,]+",
                "$1[REDACTED]"
        );
    }
}
