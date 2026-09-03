package com.example.food.admin.operation;

import com.example.food.security.AppRole;
import com.example.food.security.AuthPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

@Component
@ConditionalOnBean(AdminOperationLogService.class)
public class AdminOperationLogInterceptor implements HandlerInterceptor {

    private static final String PRINCIPAL_ATTRIBUTE = AdminOperationLogInterceptor.class.getName() + ".principal";
    private static final String LOG_ENDPOINT = "/api/admin/operation-logs";
    private static final Logger log = LoggerFactory.getLogger(AdminOperationLogInterceptor.class);

    private final AdminOperationLogService service;

    public AdminOperationLogInterceptor(AdminOperationLogService service) {
        this.service = service;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthPrincipal principal
                && principal.role() == AppRole.ADMIN) {
            request.setAttribute(PRINCIPAL_ATTRIBUTE, principal);
        }
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception exception
    ) {
        String requestPath = request.getRequestURI();
        if (LOG_ENDPOINT.equals(requestPath)) {
            return;
        }
        Object principalValue = request.getAttribute(PRINCIPAL_ATTRIBUTE);
        if (!(principalValue instanceof AuthPrincipal principal)) {
            return;
        }

        int statusCode = response.getStatus();
        String result = exception == null && statusCode < 400
                ? AdminOperationLogService.SUCCESS
                : AdminOperationLogService.FAILURE;
        try {
            service.record(
                    principal.id(),
                    principal.username(),
                    operationType(request.getMethod(), requestPath),
                    request.getMethod(),
                    requestPath,
                    result,
                    statusCode,
                    clientIp(request),
                    exception == null ? null : exception.getClass().getSimpleName()
            );
        } catch (RuntimeException logException) {
            log.warn("Unable to record admin operation {} {}", request.getMethod(), requestPath, logException);
        }
    }

    static String operationType(String method, String path) {
        if ("GET".equalsIgnoreCase(method) && "/api/admin/dashboard/overview".equals(path)) {
            return AdminOperationLogService.VIEW_DASHBOARD;
        }
        if ("GET".equalsIgnoreCase(method) && "/api/admin/ai-config/text-recipe".equals(path)) {
            return AdminOperationLogService.VIEW_AI_CONFIG;
        }
        if ("PUT".equalsIgnoreCase(method) && "/api/admin/ai-config/text-recipe".equals(path)) {
            return AdminOperationLogService.UPDATE_AI_CONFIG;
        }
        return AdminOperationLogService.ADMIN_API_ACCESS;
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return Arrays.stream(forwardedFor.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .findFirst()
                    .orElse(request.getRemoteAddr());
        }
        return request.getRemoteAddr();
    }
}
