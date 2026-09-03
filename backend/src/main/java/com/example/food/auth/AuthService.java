package com.example.food.auth;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.food.admin.AdminAccount;
import com.example.food.admin.AdminMapper;
import com.example.food.admin.operation.AdminOperationLogService;
import com.example.food.auth.dto.AdminLoginRequest;
import com.example.food.auth.dto.AuthResponse;
import com.example.food.auth.dto.PhoneLoginRequest;
import com.example.food.auth.dto.PhoneRegistrationRequest;
import com.example.food.auth.verification.SmsSendResult;
import com.example.food.auth.verification.VerificationCodeException;
import com.example.food.auth.verification.VerificationCodePurpose;
import com.example.food.auth.verification.VerificationCodeService;
import com.example.food.security.AppRole;
import com.example.food.security.AuthPrincipal;
import com.example.food.security.JwtService;
import com.example.food.user.User;
import com.example.food.user.UserMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private static final String INVALID_ADMIN_CREDENTIALS = "Invalid admin credentials";

    private final UserMapper userMapper;
    private final AdminMapper adminMapper;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final VerificationCodeService verificationCodeService;
    private final AdminOperationLogService adminOperationLogService;

    public AuthService(
            UserMapper userMapper,
            AdminMapper adminMapper,
            JwtService jwtService,
            PasswordEncoder passwordEncoder,
            VerificationCodeService verificationCodeService,
            AdminOperationLogService adminOperationLogService
    ) {
        this.userMapper = userMapper;
        this.adminMapper = adminMapper;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.verificationCodeService = verificationCodeService;
        this.adminOperationLogService = adminOperationLogService;
    }

    public SmsSendResult issueRegistrationCode(String phone) {
        if (selectUserByPhone(phone) != null) {
            throw new IllegalArgumentException("Phone already registered");
        }
        return verificationCodeService.issue(phone, VerificationCodePurpose.REGISTER);
    }

    public SmsSendResult issueLoginCode(String phone) {
        requireEnabledUser(phone);
        return verificationCodeService.issue(phone, VerificationCodePurpose.LOGIN);
    }

    @Transactional(
            isolation = Isolation.READ_COMMITTED,
            noRollbackFor = VerificationCodeException.class
    )
    public AuthResponse registerUser(PhoneRegistrationRequest request) {
        if (selectUserByPhone(request.phone()) != null) {
            throw new IllegalArgumentException("Phone already registered");
        }

        verificationCodeService.verify(
                request.phone(),
                VerificationCodePurpose.REGISTER,
                request.code()
        );

        LocalDateTime now = LocalDateTime.now();
        User user = new User();
        user.setPhone(request.phone());
        user.setNickname(request.nickname().trim());
        user.setRole(AppRole.USER.name());
        user.setEnabled(true);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.setLastLoginAt(now);
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException exception) {
            throw new IllegalArgumentException("Phone already registered");
        }
        return userResponse(user);
    }

    @Transactional(
            isolation = Isolation.READ_COMMITTED,
            noRollbackFor = VerificationCodeException.class
    )
    public AuthResponse loginUser(PhoneLoginRequest request) {
        User user = requireEnabledUser(request.phone());
        verificationCodeService.verify(request.phone(), VerificationCodePurpose.LOGIN, request.code());
        LocalDateTime now = LocalDateTime.now();
        updateEnabledUserLoginTime(user, now);
        return userResponse(user);
    }

    public AuthResponse loginAdmin(AdminLoginRequest request) {
        AdminAccount admin = adminMapper.selectOne(new QueryWrapper<AdminAccount>().eq("username", request.username()));
        if (admin == null || !Boolean.TRUE.equals(admin.getEnabled())) {
            recordAdminLogin(null, request.username(), AdminOperationLogService.FAILURE, "账号或密码错误");
            throw invalidAdminCredentials();
        }
        if (!passwordEncoder.matches(request.password(), admin.getPasswordHash())) {
            recordAdminLogin(admin, request.username(), AdminOperationLogService.FAILURE, "账号或密码错误");
            throw invalidAdminCredentials();
        }

        recordAdminLogin(admin, admin.getUsername(), AdminOperationLogService.SUCCESS, null);
        return adminResponse(admin);
    }

    private void recordAdminLogin(
            AdminAccount admin,
            String username,
            String result,
            String details
    ) {
        adminOperationLogService.record(
                admin == null ? null : admin.getId(),
                username,
                AdminOperationLogService.ADMIN_LOGIN,
                "POST",
                "/api/auth/admin/login",
                result,
                AdminOperationLogService.SUCCESS.equals(result) ? 200 : 400,
                null,
                details
        );
    }

    private User selectUserByPhone(String phone) {
        return userMapper.selectOne(new QueryWrapper<User>().eq("phone", phone));
    }

    private User requireEnabledUser(String phone) {
        User user = selectUserByPhone(phone);
        if (user == null) {
            throw new IllegalArgumentException("User not registered");
        }
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new IllegalArgumentException("User account disabled");
        }
        return user;
    }

    private void updateEnabledUserLoginTime(User user, LocalDateTime now) {
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new IllegalArgumentException("User account disabled");
        }
        user.setLastLoginAt(now);
        userMapper.updateById(user);
    }

    private AuthResponse userResponse(User user) {
        String token = jwtService.generateToken(new AuthPrincipal(user.getId(), user.getPhone(), AppRole.USER));
        return new AuthResponse(token, user.getId(), displayName(user.getNickname(), user.getPhone()), AppRole.USER.name());
    }

    private AuthResponse adminResponse(AdminAccount admin) {
        String token = jwtService.generateToken(new AuthPrincipal(admin.getId(), admin.getUsername(), AppRole.ADMIN));
        return new AuthResponse(token, admin.getId(), displayName(admin.getNickname(), admin.getUsername()), AppRole.ADMIN.name());
    }

    private String displayName(String preferredName, String fallback) {
        if (StringUtils.hasText(preferredName)) {
            return preferredName;
        }
        return fallback;
    }

    private IllegalArgumentException invalidAdminCredentials() {
        return new IllegalArgumentException(INVALID_ADMIN_CREDENTIALS);
    }
}
