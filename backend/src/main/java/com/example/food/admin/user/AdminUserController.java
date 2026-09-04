package com.example.food.admin.user;

import com.example.food.admin.operation.AdminOperationLogService;
import com.example.food.admin.user.dto.AdminUserPageResponse;
import com.example.food.admin.user.dto.AdminUserResponse;
import com.example.food.admin.user.dto.AdminUserStatusRequest;
import com.example.food.common.ApiResponse;
import com.example.food.common.RequestIp;
import com.example.food.security.AuthPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService userService;
    private final AdminOperationLogService operationLogService;

    public AdminUserController(
            AdminUserService userService,
            AdminOperationLogService operationLogService
    ) {
        this.userService = userService;
        this.operationLogService = operationLogService;
    }

    @GetMapping
    public ApiResponse<AdminUserPageResponse> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) Boolean locked,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        return ApiResponse.ok(userService.list(keyword, enabled, locked, limit, offset));
    }

    @GetMapping("/{id}/avatar")
    public ResponseEntity<byte[]> getAvatar(@PathVariable Long id) {
        AdminUserService.AvatarImage image = userService.getAvatar(id);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType(image.contentType()))
                .body(image.bytes());
    }

    @PutMapping("/{id}/status")
    public ApiResponse<AdminUserResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody(required = false) AdminUserStatusRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        try {
            AdminUserService.StatusChangeResult result = userService.updateStatus(id, request);
            operationLogService.recordUserStatusChange(
                    principal,
                    id,
                    result.maskedPhone(),
                    result.enabled(),
                    result.reason(),
                    result.changed(),
                    AdminOperationLogService.SUCCESS,
                    HttpStatus.OK.value(),
                    RequestIp.resolve(httpRequest)
            );
            return ApiResponse.ok(result.user());
        } catch (RuntimeException exception) {
            recordStatusFailure(id, request, principal, httpRequest, exception);
            throw exception;
        }
    }

    @PostMapping("/{id}/password-lock/clear")
    public ApiResponse<AdminUserResponse> clearPasswordLock(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        try {
            AdminUserService.LockClearResult result = userService.clearPasswordLock(id);
            operationLogService.recordPasswordUnlock(
                    principal,
                    id,
                    result.maskedPhone(),
                    result.changed(),
                    AdminOperationLogService.SUCCESS,
                    HttpStatus.OK.value(),
                    RequestIp.resolve(httpRequest)
            );
            return ApiResponse.ok(result.user());
        } catch (RuntimeException exception) {
            recordUnlockFailure(id, principal, httpRequest, exception);
            throw exception;
        }
    }

    private void recordStatusFailure(
            Long id,
            AdminUserStatusRequest request,
            AuthPrincipal principal,
            HttpServletRequest httpRequest,
            RuntimeException exception
    ) {
        AdminUserService.Target target = findTargetForLog(id);
        operationLogService.recordUserStatusChange(
                principal,
                id,
                target == null ? null : target.maskedPhone(),
                request != null && Boolean.TRUE.equals(request.enabled()),
                request == null ? null : request.reason(),
                false,
                AdminOperationLogService.FAILURE,
                statusCodeOf(exception),
                RequestIp.resolve(httpRequest)
        );
    }

    private void recordUnlockFailure(
            Long id,
            AuthPrincipal principal,
            HttpServletRequest httpRequest,
            RuntimeException exception
    ) {
        AdminUserService.Target target = findTargetForLog(id);
        operationLogService.recordPasswordUnlock(
                principal,
                id,
                target == null ? null : target.maskedPhone(),
                false,
                AdminOperationLogService.FAILURE,
                statusCodeOf(exception),
                RequestIp.resolve(httpRequest)
        );
    }

    private AdminUserService.Target findTargetForLog(Long id) {
        try {
            return userService.findTargetForLog(id);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private int statusCodeOf(RuntimeException exception) {
        if (exception instanceof ResponseStatusException responseStatusException) {
            return responseStatusException.getStatusCode().value();
        }
        if (exception instanceof AccessDeniedException) {
            return HttpStatus.FORBIDDEN.value();
        }
        if (exception instanceof IllegalArgumentException) {
            return HttpStatus.BAD_REQUEST.value();
        }
        return HttpStatus.INTERNAL_SERVER_ERROR.value();
    }
}
