package com.example.food.auth;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.food.auth.dto.AuthResponse;
import com.example.food.auth.dto.PasswordResetRequest;
import com.example.food.auth.dto.PhoneLoginRequest;
import com.example.food.auth.dto.UserPasswordLoginRequest;
import com.example.food.auth.verification.PhoneVerificationCode;
import com.example.food.auth.verification.PhoneVerificationCodeMapper;
import com.example.food.user.User;
import com.example.food.user.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class AuthPasswordServiceTest {

    private static final String TEST_PHONE_PREFIX = "13900003";
    private static final String PASSWORD = "Recipe123";
    private static final String WRONG_PASSWORD = "Wrong123";

    @Autowired
    private AuthService authService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PhoneVerificationCodeMapper verificationCodeMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanTestData() {
        verificationCodeMapper.delete(new QueryWrapper<PhoneVerificationCode>()
                .likeRight("phone", TEST_PHONE_PREFIX));
        userMapper.delete(new QueryWrapper<User>().likeRight("phone", TEST_PHONE_PREFIX));
    }

    @Test
    void registeredUserCanLoginWithPassword() {
        User user = insertUser("13900003001", PASSWORD);

        AuthResponse response = authService.loginUserWithPassword(
                new UserPasswordLoginRequest(user.getPhone(), PASSWORD)
        );

        assertThat(response.id()).isEqualTo(user.getId());
        assertThat(response.token()).isNotBlank();
        User stored = userMapper.selectById(user.getId());
        assertThat(stored.getPasswordFailedAttempts()).isZero();
        assertThat(stored.getPasswordLockedUntil()).isNull();
        assertThat(stored.getLastLoginAt()).isNotNull();
    }

    @Test
    void unknownAndLegacyUsersReceiveTheSamePasswordError() {
        User legacyUser = insertUser("13900003002", null);

        assertThatThrownBy(() -> authService.loginUserWithPassword(
                new UserPasswordLoginRequest("13900003003", PASSWORD)
        ))
                .isInstanceOf(PasswordLoginFailureException.class)
                .hasMessage("User phone or password invalid");
        assertThatThrownBy(() -> authService.loginUserWithPassword(
                new UserPasswordLoginRequest(legacyUser.getPhone(), PASSWORD)
        ))
                .isInstanceOf(PasswordLoginFailureException.class)
                .hasMessage("User phone or password invalid");
    }

    @Test
    void fifthFailureLocksOnlyPasswordLoginAndCodeLoginStillWorks() {
        User user = insertUser("13900003004", PASSWORD);

        for (int attempt = 1; attempt <= 4; attempt++) {
            assertThatThrownBy(() -> authService.loginUserWithPassword(
                    new UserPasswordLoginRequest(user.getPhone(), WRONG_PASSWORD)
            ))
                    .isInstanceOf(PasswordLoginFailureException.class)
                    .hasMessage("User phone or password invalid");
            assertThat(userMapper.selectById(user.getId()).getPasswordFailedAttempts())
                    .isEqualTo(attempt);
        }

        assertThatThrownBy(() -> authService.loginUserWithPassword(
                new UserPasswordLoginRequest(user.getPhone(), WRONG_PASSWORD)
        ))
                .isInstanceOf(PasswordLoginLockedException.class)
                .hasMessage("Password login locked; retry after 900 seconds");

        User locked = userMapper.selectById(user.getId());
        assertThat(locked.getPasswordFailedAttempts()).isEqualTo(5);
        assertThat(locked.getPasswordLockedUntil()).isAfter(LocalDateTime.now());

        assertThatThrownBy(() -> authService.loginUserWithPassword(
                new UserPasswordLoginRequest(user.getPhone(), PASSWORD)
        ))
                .isInstanceOf(PasswordLoginLockedException.class);

        String code = authService.issueLoginCode(user.getPhone()).code();
        assertThat(authService.loginUser(new PhoneLoginRequest(user.getPhone(), code)).id())
                .isEqualTo(user.getId());
    }

    @Test
    void expiredLockAutomaticallyUnlocksOnNextPasswordAttempt() {
        User user = insertUser("13900003005", PASSWORD);
        user.setPasswordFailedAttempts(5);
        user.setPasswordLockedUntil(LocalDateTime.now().minusSeconds(1));
        userMapper.updatePasswordState(user);

        assertThat(authService.loginUserWithPassword(
                new UserPasswordLoginRequest(user.getPhone(), PASSWORD)
        ).id()).isEqualTo(user.getId());

        User stored = userMapper.selectById(user.getId());
        assertThat(stored.getPasswordFailedAttempts()).isZero();
        assertThat(stored.getPasswordLockedUntil()).isNull();
    }

    @Test
    void passwordResetClearsFailuresAndAllowsNewPassword() {
        User user = insertUser("13900003006", PASSWORD);
        lockUser(user);

        String resetCode = authService.issuePasswordResetCode(user.getPhone()).code();
        authService.resetUserPassword(new PasswordResetRequest(
                user.getPhone(), resetCode, "NewRecipe456"
        ));

        User stored = userMapper.selectById(user.getId());
        assertThat(stored.getPasswordFailedAttempts()).isZero();
        assertThat(stored.getPasswordLockedUntil()).isNull();
        assertThat(passwordEncoder.matches("NewRecipe456", stored.getPasswordHash())).isTrue();
        assertThat(authService.loginUserWithPassword(
                new UserPasswordLoginRequest(user.getPhone(), "NewRecipe456")
        ).id()).isEqualTo(user.getId());
    }

    @Test
    void verificationCodePurposesAreIsolatedForPasswordReset() {
        User user = insertUser("13900003007", PASSWORD);
        String loginCode = authService.issueLoginCode(user.getPhone()).code();

        assertThatThrownBy(() -> authService.resetUserPassword(new PasswordResetRequest(
                user.getPhone(), loginCode, "NewRecipe456"
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Verification code invalid");

        assertThat(authService.loginUser(new PhoneLoginRequest(user.getPhone(), loginCode)).id())
                .isEqualTo(user.getId());
    }

    @Test
    void concurrentWrongPasswordsDoNotLoseFailureCounts() throws Exception {
        User user = insertUser("13900003008", PASSWORD);
        int requestCount = 6;
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        try {
            List<Callable<Throwable>> calls = new ArrayList<>();
            for (int index = 0; index < requestCount; index++) {
                calls.add(() -> {
                    ready.countDown();
                    start.await();
                    try {
                        authService.loginUserWithPassword(
                                new UserPasswordLoginRequest(user.getPhone(), WRONG_PASSWORD)
                        );
                        return null;
                    } catch (Throwable throwable) {
                        return throwable;
                    }
                });
            }
            List<Future<Throwable>> futures = new ArrayList<>();
            for (Callable<Throwable> call : calls) {
                futures.add(executor.submit(call));
            }
            ready.await();
            start.countDown();
            for (Future<Throwable> future : futures) {
                assertThat(future.get()).isInstanceOf(IllegalArgumentException.class);
            }
        } finally {
            executor.shutdownNow();
        }

        User stored = userMapper.selectById(user.getId());
        assertThat(stored.getPasswordFailedAttempts()).isEqualTo(5);
        assertThat(stored.getPasswordLockedUntil()).isAfter(LocalDateTime.now());
    }

    private void lockUser(User user) {
        for (int attempt = 0; attempt < 5; attempt++) {
            assertThatThrownBy(() -> authService.loginUserWithPassword(
                    new UserPasswordLoginRequest(user.getPhone(), WRONG_PASSWORD)
            )).isInstanceOf(IllegalArgumentException.class);
        }
    }

    private User insertUser(String phone, String password) {
        LocalDateTime now = LocalDateTime.now();
        User user = new User();
        user.setPhone(phone);
        user.setNickname(phone);
        user.setRole("USER");
        user.setEnabled(true);
        user.setPasswordHash(password == null ? null : passwordEncoder.encode(password));
        user.setPasswordFailedAttempts(0);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userMapper.insert(user);
        return user;
    }
}
