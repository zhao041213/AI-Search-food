package com.example.food.admin.user;

import com.example.food.review.UploadedFile;
import com.example.food.review.UploadedFileMapper;
import com.example.food.security.AppRole;
import com.example.food.user.User;
import com.example.food.user.UserMapper;
import com.example.food.admin.user.dto.AdminUserPageResponse;
import com.example.food.admin.user.dto.AdminUserResponse;
import com.example.food.admin.user.dto.AdminUserStatusRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminUserService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;
    private static final int MAX_KEYWORD_LENGTH = 64;
    private static final int MAX_REASON_LENGTH = 255;

    private final UserMapper userMapper;
    private final UploadedFileMapper uploadedFileMapper;
    private final com.example.food.user.account.UserAvatarFileStorage avatarStorage;

    public AdminUserService(
            UserMapper userMapper,
            UploadedFileMapper uploadedFileMapper,
            com.example.food.user.account.UserAvatarFileStorage avatarStorage
    ) {
        this.userMapper = userMapper;
        this.uploadedFileMapper = uploadedFileMapper;
        this.avatarStorage = avatarStorage;
    }

    public AdminUserPageResponse list(String keyword, Boolean enabled, Boolean locked, int limit, int offset) {
        String normalizedKeyword = normalizeKeyword(keyword);
        int safeLimit = limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        int safeOffset = Math.max(0, offset);
        LocalDateTime now = LocalDateTime.now();
        List<AdminUserResponse> items = userMapper.findAdminPage(
                        normalizedKeyword, enabled, locked, now, safeLimit, safeOffset
                ).stream()
                .map(user -> toResponse(user, now))
                .toList();
        return new AdminUserPageResponse(
                items,
                userMapper.countAdminUsers(normalizedKeyword, enabled, locked, now),
                safeLimit,
                safeOffset
        );
    }

    @Transactional
    public StatusChangeResult updateStatus(Long id, AdminUserStatusRequest request) {
        if (request == null || request.enabled() == null) {
            throw new IllegalArgumentException("账号状态不能为空");
        }
        String reason = normalizeReason(request.reason(), !request.enabled());
        User user = requireUserForUpdate(id);
        boolean currentEnabled = Boolean.TRUE.equals(user.getEnabled());
        boolean changed = currentEnabled != request.enabled();
        if (changed) {
            LocalDateTime now = LocalDateTime.now();
            user.setEnabled(request.enabled());
            user.setAuthVersion(currentAuthVersion(user) + (request.enabled() ? 0 : 1));
            user.setUpdatedAt(now);
            if (userMapper.updateAdminStatus(user) != 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "用户状态已变化，请刷新后重试");
            }
        }
        return new StatusChangeResult(
                toResponse(user, LocalDateTime.now()),
                maskPhone(user.getPhone()),
                request.enabled(),
                reason,
                changed
        );
    }

    @Transactional
    public LockClearResult clearPasswordLock(Long id) {
        User user = requireUserForUpdate(id);
        boolean needsClear = (user.getPasswordFailedAttempts() != null && user.getPasswordFailedAttempts() > 0)
                || user.getPasswordLockedUntil() != null;
        if (!needsClear) {
            return new LockClearResult(toResponse(user, LocalDateTime.now()), maskPhone(user.getPhone()), false);
        }
        LocalDateTime now = LocalDateTime.now();
        if (userMapper.clearPasswordLock(id, now) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "密码锁定状态已变化，请刷新后重试");
        }
        user.setPasswordFailedAttempts(0);
        user.setPasswordLockedUntil(null);
        user.setUpdatedAt(now);
        return new LockClearResult(toResponse(user, now), maskPhone(user.getPhone()), true);
    }

    public AvatarImage getAvatar(Long id) {
        User user = requireUser(id);
        if (!StringUtils.hasText(user.getAvatarUrl())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "当前用户没有头像");
        }
        UploadedFile file = uploadedFileMapper.selectLatestByUserAndPurpose(
                id,
                com.example.food.user.account.UserAvatarFileStorage.PURPOSE
        );
        if (file == null || !user.getAvatarUrl().equals(file.getStoredName())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "头像文件不存在");
        }
        return new AvatarImage(avatarStorage.load(file.getStoredName()), file.getContentType());
    }

    public Target findTargetForLog(Long id) {
        if (id == null) {
            return null;
        }
        User user = userMapper.selectById(id);
        if (user == null || !AppRole.USER.name().equals(user.getRole())) {
            return null;
        }
        return new Target(id, maskPhone(user.getPhone()));
    }

    public static String maskPhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return "暂无";
        }
        String normalized = phone.trim();
        if (normalized.length() <= 7) {
            return "***";
        }
        return normalized.substring(0, 3) + "****" + normalized.substring(normalized.length() - 4);
    }

    private User requireUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null || !AppRole.USER.name().equals(user.getRole()) || user.getDeletedAt() != null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "普通用户不存在");
        }
        return user;
    }

    private User requireUserForUpdate(Long id) {
        User user = userMapper.selectByIdForUpdate(id);
        if (user == null || !AppRole.USER.name().equals(user.getRole()) || user.getDeletedAt() != null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "普通用户不存在");
        }
        return user;
    }

    private AdminUserResponse toResponse(User user, LocalDateTime now) {
        boolean hasAvatar = StringUtils.hasText(user.getAvatarUrl());
        return new AdminUserResponse(
                user.getId(),
                user.getNickname(),
                maskPhone(user.getPhone()),
                hasAvatar ? "/api/admin/users/" + user.getId() + "/avatar" : null,
                Boolean.TRUE.equals(user.getEnabled()),
                isPasswordLocked(user, now),
                user.getCreatedAt(),
                user.getLastLoginAt()
        );
    }

    private boolean isPasswordLocked(User user) {
        return isPasswordLocked(user, LocalDateTime.now());
    }

    private boolean isPasswordLocked(User user, LocalDateTime now) {
        return user.getPasswordLockedUntil() != null && user.getPasswordLockedUntil().isAfter(now);
    }

    private String normalizeKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return limitText(keyword.trim(), MAX_KEYWORD_LENGTH);
    }

    private String normalizeReason(String reason, boolean required) {
        String normalized = reason == null ? "" : reason.trim();
        if (required && normalized.isBlank()) {
            throw new IllegalArgumentException("禁用原因不能为空");
        }
        if (normalized.codePointCount(0, normalized.length()) > MAX_REASON_LENGTH) {
            throw new IllegalArgumentException("操作原因不能超过 255 个字符");
        }
        return normalized;
    }

    private int currentAuthVersion(User user) {
        return user.getAuthVersion() == null ? 0 : user.getAuthVersion();
    }

    private String limitText(String value, int maxLength) {
        int end = value.offsetByCodePoints(0, Math.min(value.codePointCount(0, value.length()), maxLength));
        return value.substring(0, end);
    }

    public record StatusChangeResult(
            AdminUserResponse user,
            String maskedPhone,
            boolean enabled,
            String reason,
            boolean changed
    ) {
    }

    public record Target(Long id, String maskedPhone) {
    }

    public record LockClearResult(
            AdminUserResponse user,
            String maskedPhone,
            boolean changed
    ) {
    }

    public record AvatarImage(byte[] bytes, String contentType) {
    }
}
