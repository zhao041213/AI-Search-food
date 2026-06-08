package com.example.food.auth;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.food.admin.AdminAccount;
import com.example.food.admin.AdminMapper;
import com.example.food.auth.dto.AdminLoginRequest;
import com.example.food.auth.dto.AuthResponse;
import com.example.food.auth.dto.PhoneLoginRequest;
import com.example.food.security.AppRole;
import com.example.food.security.AuthPrincipal;
import com.example.food.security.JwtService;
import com.example.food.user.User;
import com.example.food.user.UserMapper;
import org.springframework.beans.factory.annotation.Value;
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
    private final String mockCode;

    public AuthService(
            UserMapper userMapper,
            AdminMapper adminMapper,
            JwtService jwtService,
            PasswordEncoder passwordEncoder,
            @Value("${app.auth.mock-code}") String mockCode
    ) {
        this.userMapper = userMapper;
        this.adminMapper = adminMapper;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.mockCode = mockCode;
    }

    public String issueMockCode(String phone) {
        if (!StringUtils.hasText(phone)) {
            throw new IllegalArgumentException("Phone is required");
        }
        return mockCode;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AuthResponse loginUser(PhoneLoginRequest request) {
        if (!mockCode.equals(request.code())) {
            throw new IllegalArgumentException("Invalid verification code");
        }

        LocalDateTime now = LocalDateTime.now();
        User user = selectUserByPhone(request.phone());
        if (user == null) {
            user = createUser(request.phone(), now);
        } else {
            updateEnabledUserLoginTime(user, now);
        }

        return userResponse(user);
    }

    public AuthResponse loginAdmin(AdminLoginRequest request) {
        AdminAccount admin = adminMapper.selectOne(new QueryWrapper<AdminAccount>().eq("username", request.username()));
        if (admin == null || !Boolean.TRUE.equals(admin.getEnabled())) {
            throw invalidAdminCredentials();
        }
        if (!passwordEncoder.matches(request.password(), admin.getPasswordHash())) {
            throw invalidAdminCredentials();
        }

        return adminResponse(admin);
    }

    private User createUser(String phone, LocalDateTime now) {
        User user = new User();
        user.setPhone(phone);
        user.setNickname(phone);
        user.setRole(AppRole.USER.name());
        user.setEnabled(true);
        user.setLastLoginAt(now);
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException exception) {
            User existingUser = selectUserByPhone(phone);
            if (existingUser == null) {
                throw exception;
            }
            updateEnabledUserLoginTime(existingUser, now);
            return existingUser;
        }
        return user;
    }

    private User selectUserByPhone(String phone) {
        return userMapper.selectOne(new QueryWrapper<User>().eq("phone", phone));
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
