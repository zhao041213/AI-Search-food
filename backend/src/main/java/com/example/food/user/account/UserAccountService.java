package com.example.food.user.account;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.food.auth.verification.SmsSendResult;
import com.example.food.auth.verification.VerificationCodePurpose;
import com.example.food.auth.verification.VerificationCodeService;
import com.example.food.common.RequestIp;
import com.example.food.review.UploadedFile;
import com.example.food.review.UploadedFileMapper;
import com.example.food.security.AppRole;
import com.example.food.user.User;
import com.example.food.user.UserMapper;
import com.example.food.user.account.dto.AccountCancellationRequest;
import com.example.food.user.account.dto.UserAccountResponse;
import com.example.food.user.account.dto.UserProfileUpdateRequest;
import com.example.food.user.security.UserSecurityEventService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class UserAccountService {
    private final UserMapper userMapper;
    private final UploadedFileMapper uploadedFileMapper;
    private final UserAvatarFileStorage avatarStorage;
    private final VerificationCodeService verificationCodeService;
    private final UserSecurityEventService securityEventService;

    public UserAccountService(
            UserMapper userMapper,
            UploadedFileMapper uploadedFileMapper,
            UserAvatarFileStorage avatarStorage,
            VerificationCodeService verificationCodeService,
            UserSecurityEventService securityEventService
    ) {
        this.userMapper = userMapper;
        this.uploadedFileMapper = uploadedFileMapper;
        this.avatarStorage = avatarStorage;
        this.verificationCodeService = verificationCodeService;
        this.securityEventService = securityEventService;
    }

    public UserAccountResponse getAccount(Long userId) {
        return toResponse(requireActiveUser(userId));
    }

    @Transactional
    public UserAccountResponse updateProfile(Long userId, UserProfileUpdateRequest request, String ipAddress) {
        User user = requireActiveUser(userId);
        String nickname = request.nickname().trim();
        if (nickname.codePointCount(0, nickname.length()) < 2 || nickname.codePointCount(0, nickname.length()) > 64) {
            throw new IllegalArgumentException("昵称长度需为 2-64 个字符");
        }
        user.setNickname(nickname);
        user.setUpdatedAt(LocalDateTime.now());
        if (userMapper.updateNickname(user) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "账号状态已变化，请重新登录");
        }
        securityEventService.record(userId, UserSecurityEventService.PROFILE_UPDATED,
                UserSecurityEventService.SUCCESS, ipAddress, "昵称已更新");
        return toResponse(user);
    }

    @Transactional
    public UserAccountResponse uploadAvatar(Long userId, MultipartFile file, String ipAddress) {
        User user = requireActiveUser(userId);
        UploadedFile oldFile = latestAvatar(userId);
        UserAvatarFileStorage.StoredAvatar stored = avatarStorage.store(file);
        boolean committed = false;
        try {
            UploadedFile uploadedFile = new UploadedFile();
            uploadedFile.setUserId(userId);
            uploadedFile.setOriginalName(stored.originalName());
            uploadedFile.setStoredName(stored.storedName());
            uploadedFile.setContentType(stored.contentType());
            uploadedFile.setFileSize(stored.fileSize());
            uploadedFile.setStoragePath(stored.path().toString());
            uploadedFile.setPurpose(UserAvatarFileStorage.PURPOSE);
            uploadedFile.setCreatedAt(LocalDateTime.now());
            uploadedFileMapper.insert(uploadedFile);

            user.setAvatarUrl(stored.storedName());
            user.setUpdatedAt(LocalDateTime.now());
            if (userMapper.updateAvatar(user) != 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "账号状态已变化，请重新登录");
            }
            deleteOldAvatarRecord(oldFile);
            registerAvatarCleanup(stored.storedName(), oldFile == null ? null : oldFile.getStoredName());
            securityEventService.record(userId, UserSecurityEventService.AVATAR_UPDATED,
                    UserSecurityEventService.SUCCESS, ipAddress, "头像已更新");
            committed = true;
            return toResponse(user);
        } finally {
            if (!committed) {
                avatarStorage.deleteQuietly(stored.storedName());
            }
        }
    }

    @Transactional
    public UserAccountResponse deleteAvatar(Long userId, String ipAddress) {
        User user = requireActiveUser(userId);
        UploadedFile oldFile = latestAvatar(userId);
        user.setAvatarUrl(null);
        user.setUpdatedAt(LocalDateTime.now());
        if (userMapper.updateAvatar(user) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "账号状态已变化，请重新登录");
        }
        if (oldFile != null) {
            uploadedFileMapper.deleteById(oldFile.getId());
            registerAvatarCleanup(null, oldFile.getStoredName());
        }
        securityEventService.record(userId, UserSecurityEventService.AVATAR_UPDATED,
                UserSecurityEventService.SUCCESS, ipAddress, "头像已删除");
        return toResponse(user);
    }

    public AvatarImage getAvatar(Long userId) {
        User user = requireActiveUser(userId);
        if (user.getAvatarUrl() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "当前账号没有头像");
        }
        UploadedFile file = latestAvatar(userId);
        if (file == null || !user.getAvatarUrl().equals(file.getStoredName())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "头像文件不存在");
        }
        return new AvatarImage(avatarStorage.load(file.getStoredName()), file.getContentType());
    }

    @Transactional
    public void logoutAllDevices(Long userId, String ipAddress) {
        User user = requireActiveUserForUpdate(userId);
        user.setAuthVersion(currentAuthVersion(user) + 1);
        user.setUpdatedAt(LocalDateTime.now());
        if (userMapper.updateById(user) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "账号状态已变化，请重新登录");
        }
        securityEventService.record(userId, UserSecurityEventService.LOGOUT_ALL_DEVICES,
                UserSecurityEventService.SUCCESS, ipAddress, "已使全部设备下线");
    }

    public SmsSendResult issueCancellationCode(Long userId) {
        User user = requireActiveUser(userId);
        return verificationCodeService.issue(user.getPhone(), VerificationCodePurpose.CANCEL_ACCOUNT);
    }

    @Transactional
    public void cancelAccount(Long userId, AccountCancellationRequest request, String ipAddress) {
        User user = requireActiveUserForUpdate(userId);
        verificationCodeService.verify(user.getPhone(), VerificationCodePurpose.CANCEL_ACCOUNT, request.code());
        UploadedFile avatar = latestAvatar(userId);
        LocalDateTime now = LocalDateTime.now();
        user.setEnabled(false);
        user.setDeletedAt(now);
        user.setAuthVersion(currentAuthVersion(user) + 1);
        user.setPasswordHash(null);
        user.setPasswordFailedAttempts(0);
        user.setPasswordLockedUntil(null);
        user.setAvatarUrl(null);
        user.setPhone(deletedPhone(userId));
        user.setNickname("已注销用户");
        user.setUpdatedAt(now);
        if (userMapper.cancelAccount(user) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "账号状态已变化，请重新登录");
        }
        List<UploadedFile> avatarFiles = uploadedFileMapper.selectByUserAndPurpose(userId, UserAvatarFileStorage.PURPOSE);
        for (UploadedFile file : avatarFiles) {
            uploadedFileMapper.deleteById(file.getId());
        }
        registerAvatarCleanup(null, avatarFiles.stream().map(UploadedFile::getStoredName).toList());
        securityEventService.record(userId, UserSecurityEventService.ACCOUNT_CANCELLED,
                UserSecurityEventService.SUCCESS, ipAddress, "账号已注销");
    }

    private User requireActiveUser(Long userId) {
        User user = userMapper.selectById(userId);
        return activeUser(user);
    }

    private User requireActiveUserForUpdate(Long userId) {
        User user = userMapper.selectByIdForUpdate(userId);
        return activeUser(user);
    }

    private User activeUser(User user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "用户账号不存在");
        }
        if (!AppRole.USER.name().equals(user.getRole())) {
            throw new AccessDeniedException("仅普通用户可访问账号中心");
        }
        if (!Boolean.TRUE.equals(user.getEnabled()) || user.getDeletedAt() != null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "账号已停用或注销");
        }
        return user;
    }

    private UploadedFile latestAvatar(Long userId) {
        return uploadedFileMapper.selectLatestByUserAndPurpose(userId, UserAvatarFileStorage.PURPOSE);
    }

    private void deleteOldAvatarRecord(UploadedFile oldFile) {
        if (oldFile != null) {
            uploadedFileMapper.deleteById(oldFile.getId());
        }
    }

    private void registerAvatarCleanup(String newFile, String oldFile) {
        registerAvatarCleanup(newFile, oldFile == null ? List.of() : List.of(oldFile));
    }

    private void registerAvatarCleanup(String newFile, List<String> oldFiles) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            oldFiles.forEach(avatarStorage::deleteQuietly);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_COMMITTED) {
                    oldFiles.forEach(avatarStorage::deleteQuietly);
                } else if (newFile != null) {
                    avatarStorage.deleteQuietly(newFile);
                }
            }
        });
    }

    private int currentAuthVersion(User user) {
        return user.getAuthVersion() == null ? 0 : user.getAuthVersion();
    }

    private String deletedPhone(Long userId) {
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        return "deleted-" + userId + "-" + random;
    }

    private UserAccountResponse toResponse(User user) {
        String avatarUrl = user.getAvatarUrl() == null ? null : "/api/users/me/account/avatar";
        String status = Boolean.TRUE.equals(user.getEnabled()) && user.getDeletedAt() == null ? "ACTIVE" : "DISABLED";
        return new UserAccountResponse(
                user.getId(), user.getNickname(), user.getPhone(), avatarUrl,
                user.getCreatedAt(), user.getLastLoginAt(), status
        );
    }

    public record AvatarImage(byte[] bytes, String contentType) {
    }
}
