package com.example.food.admin.error;

import com.example.food.admin.error.dto.AdminErrorLogPageResponse;
import com.example.food.admin.error.dto.AdminErrorLogResponse;
import com.example.food.security.AppRole;
import com.example.food.security.AuthPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class AdminErrorLogService {

    public static final String BACKEND = "BACKEND";
    public static final String AI = "AI";
    public static final String DATABASE = "DATABASE";
    public static final String TOOL = "TOOL";
    public static final String ERROR = "ERROR";

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;
    private static final int MAX_STACK_TRACE_LENGTH = 12_000;
    private static final Logger log = LoggerFactory.getLogger(AdminErrorLogService.class);

    private final AdminErrorLogMapper mapper;

    public AdminErrorLogService(AdminErrorLogMapper mapper) {
        this.mapper = mapper;
    }

    public void recordException(Throwable exception, HttpServletRequest request, int statusCode) {
        AuthPrincipal principal = currentPrincipal();
        recordException(
                exception,
                principal,
                request == null ? null : request.getMethod(),
                request == null ? null : request.getRequestURI(),
                statusCode,
                request == null ? null : clientIp(request)
        );
    }

    public void recordException(
            Throwable exception,
            AuthPrincipal principal,
            String requestMethod,
            String requestPath,
            Integer statusCode,
            String ipAddress
    ) {
        record(
                sourceOf(exception),
                componentOf(exception),
                exception == null ? null : exception.getMessage(),
                exception,
                principal,
                requestMethod,
                requestPath,
                statusCode,
                ipAddress
        );
    }

    public void recordFailure(
            String sourceType,
            String component,
            String message,
            Throwable exception,
            AuthPrincipal principal,
            String requestMethod,
            String requestPath,
            Integer statusCode,
            String ipAddress
    ) {
        record(sourceType, component, message, exception, principal, requestMethod, requestPath, statusCode, ipAddress);
    }

    public AdminErrorLogPageResponse list(
            String sourceType,
            String keyword,
            LocalDate from,
            LocalDate to,
            int limit,
            int offset
    ) {
        String normalizedSource = normalizeSource(sourceType);
        String normalizedKeyword = normalizeKeyword(keyword);
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("异常时间范围不合法");
        }
        LocalDateTime fromTime = from == null ? null : from.atStartOfDay();
        LocalDateTime toTime = to == null ? null : to.plusDays(1).atStartOfDay();
        int safeLimit = limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        int safeOffset = Math.max(0, offset);
        List<AdminErrorLogResponse> items = mapper.findPage(
                        normalizedSource,
                        normalizedKeyword,
                        fromTime,
                        toTime,
                        safeLimit,
                        safeOffset
                ).stream()
                .map(this::responseOf)
                .toList();
        return new AdminErrorLogPageResponse(
                items,
                mapper.count(normalizedSource, normalizedKeyword, fromTime, toTime),
                safeLimit,
                safeOffset
        );
    }

    public AdminErrorLogResponse find(Long id) {
        return id == null ? null : responseOf(mapper.selectDetail(id));
    }

    private void record(
            String sourceType,
            String component,
            String message,
            Throwable exception,
            AuthPrincipal principal,
            String requestMethod,
            String requestPath,
            Integer statusCode,
            String ipAddress
    ) {
        AdminErrorLog record = new AdminErrorLog();
        record.setSourceType(normalizeSource(sourceType));
        record.setSeverity(ERROR);
        record.setComponent(limitText(valueOrFallback(component, componentOf(exception)), 128));
        record.setExceptionClass(exception == null ? null : limitText(exception.getClass().getName(), 255));
        record.setMessage(limitText(sanitize(valueOrFallback(message, "未提供错误信息")), 1000));
        record.setRootCause(limitText(sanitize(rootCause(exception)), 1000));
        record.setRequestMethod(limitText(requestMethod, 16));
        record.setRequestPath(limitText(requestPath, 255));
        record.setStatusCode(statusCode);
        applyPrincipal(record, principal);
        record.setIpAddress(limitText(ipAddress, 64));
        record.setStackTrace(limitText(sanitize(stackTrace(exception)), MAX_STACK_TRACE_LENGTH));
        record.setCreatedAt(LocalDateTime.now());
        try {
            mapper.insert(record);
        } catch (RuntimeException persistenceException) {
            // Error logging must never turn the original request or async task into another failure.
            log.warn("Unable to persist system error log for {}", record.getComponent(), persistenceException);
        }
    }

    private void applyPrincipal(AdminErrorLog record, AuthPrincipal principal) {
        if (principal == null || principal.id() == null || principal.role() == null) {
            return;
        }
        if (principal.role() == AppRole.ADMIN) {
            record.setAdminId(principal.id());
        } else if (principal.role() == AppRole.USER) {
            record.setUserId(principal.id());
        }
    }

    private AdminErrorLogResponse responseOf(AdminErrorLog record) {
        if (record == null) {
            return null;
        }
        return new AdminErrorLogResponse(
                record.getId(),
                record.getSourceType(),
                record.getSeverity(),
                record.getComponent(),
                record.getExceptionClass(),
                record.getMessage(),
                record.getRootCause(),
                record.getRequestMethod(),
                record.getRequestPath(),
                record.getStatusCode(),
                record.getUserId(),
                record.getAdminId(),
                record.getIpAddress(),
                record.getStackTrace(),
                record.getCreatedAt()
        );
    }

    private String normalizeSource(String sourceType) {
        if (!StringUtils.hasText(sourceType)) {
            return null;
        }
        String normalized = sourceType.trim().toUpperCase(Locale.ROOT);
        if (!List.of(BACKEND, AI, DATABASE, TOOL).contains(normalized)) {
            throw new IllegalArgumentException("异常来源不合法");
        }
        return normalized;
    }

    private String normalizeKeyword(String keyword) {
        return StringUtils.hasText(keyword) ? limitText(keyword.trim(), 64) : null;
    }

    private String sourceOf(Throwable exception) {
        String signal = signalOf(exception);
        if (signal.contains("org.springframework.dao.")
                || signal.contains("dataaccess")
                || signal.contains("sqlexception")
                || signal.contains("sqlgrammar")
                || signal.contains("sqlintegrity")
                || signal.contains("jdbc")
                || signal.contains("mybatis")) {
            return DATABASE;
        }
        if (signal.contains("com.example.food.ai.")
                || signal.contains("qwen")
                || signal.contains("千问")
                || signal.contains("模型")) {
            return AI;
        }
        if (signal.contains("com.example.food.stats.image.")
                || signal.contains("wikimedia")
                || signal.contains("tool")
                || signal.contains("工具")) {
            return TOOL;
        }
        return BACKEND;
    }

    private String componentOf(Throwable exception) {
        if (exception == null) {
            return "unknown";
        }
        for (Throwable current = exception; current != null; current = current.getCause()) {
            for (StackTraceElement element : current.getStackTrace()) {
                if (element.getClassName().startsWith("com.example.food.")) {
                    return limitText(simpleClassName(element.getClassName()) + "#" + element.getMethodName(), 128);
                }
            }
        }
        return limitText(exception.getClass().getSimpleName(), 128);
    }

    private String signalOf(Throwable exception) {
        if (exception == null) {
            return "";
        }
        StringBuilder signal = new StringBuilder();
        for (Throwable current = exception; current != null; current = current.getCause()) {
            signal.append(current.getClass().getName()).append(' ');
            if (current.getMessage() != null) {
                signal.append(current.getMessage()).append(' ');
            }
            for (StackTraceElement element : current.getStackTrace()) {
                signal.append(element.getClassName()).append(' ');
            }
        }
        return signal.toString().toLowerCase(Locale.ROOT);
    }

    private String rootCause(Throwable exception) {
        if (exception == null) {
            return null;
        }
        Throwable root = exception;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String message = root.getMessage();
        return root.getClass().getName() + (StringUtils.hasText(message) ? ": " + message : "");
    }

    private String stackTrace(Throwable exception) {
        if (exception == null) {
            return null;
        }
        StringWriter writer = new StringWriter();
        exception.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    private String sanitize(String value) {
        if (value == null) {
            return null;
        }
        String sanitized = value
                .replaceAll("(?i)(\\\"?(?:api[-_ ]?key|password|token|secret|authorization|access[-_ ]?key)\\\"?\\s*:\\s*\\\"?)([^\\\"\\s,}]+)", "$1[已脱敏]")
                .replaceAll("(?i)((?:api[-_ ]?key|password|token|secret|authorization|access[-_ ]?key)\\s*[=:]\\s*)([^\\s,;]+)", "$1[已脱敏]")
                .replaceAll("(?i)Bearer\\s+[A-Za-z0-9._~+/=-]+", "Bearer [已脱敏]")
                .replaceAll("(?i)(https?://[^\\s?]+)\\?[^\\s]+", "$1?[已脱敏]");
        return sanitized;
    }

    private AuthPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof AuthPrincipal principal
                ? principal
                : null;
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",", 2)[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String valueOrFallback(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String simpleClassName(String className) {
        int separator = className.lastIndexOf('.');
        return separator >= 0 ? className.substring(separator + 1) : className;
    }

    private String limitText(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
