package com.example.food.notification;

import com.example.food.common.ApiResponse;
import com.example.food.notification.dto.NotificationPageResponse;
import com.example.food.notification.dto.NotificationPreferenceRequest;
import com.example.food.notification.dto.NotificationPreferenceResponse;
import com.example.food.notification.dto.NotificationResponse;
import com.example.food.security.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping("/notifications")
    public ApiResponse<NotificationPageResponse> list(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok(service.list(principal.id(), status, page, size));
    }

    @GetMapping("/notifications/unread-count")
    public ApiResponse<Long> unreadCount(@AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.ok(service.unreadCount(principal.id()));
    }

    @GetMapping("/notifications/{id}")
    public ApiResponse<NotificationResponse> detail(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long id
    ) {
        return ApiResponse.ok(service.detail(principal.id(), id));
    }

    @PutMapping("/notifications/{id}/read")
    public ApiResponse<NotificationResponse> markRead(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long id
    ) {
        return ApiResponse.ok(service.markRead(principal.id(), id));
    }

    @PutMapping("/notifications/read-all")
    public ApiResponse<Long> markAllRead(@AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.ok(service.markAllRead(principal.id()));
    }

    @PutMapping("/notifications/{id}/archive")
    public ApiResponse<NotificationResponse> archive(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long id
    ) {
        return ApiResponse.ok(service.archive(principal.id(), id));
    }

    @GetMapping("/notification-preferences")
    public ApiResponse<NotificationPreferenceResponse> getPreferences(
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return ApiResponse.ok(service.getPreferences(principal.id()));
    }

    @PutMapping("/notification-preferences")
    public ApiResponse<NotificationPreferenceResponse> updatePreferences(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody NotificationPreferenceRequest request
    ) {
        return ApiResponse.ok(service.updatePreferences(principal.id(), request));
    }
}
