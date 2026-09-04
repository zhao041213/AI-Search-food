package com.example.food.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

import java.util.Arrays;

public final class RequestIp {
    private RequestIp() {
    }

    public static String resolve(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
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
