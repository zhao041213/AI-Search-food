package com.example.food.auth;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.food.auth.dto.AuthResponse;
import com.example.food.auth.dto.PhoneLoginRequest;
import com.example.food.auth.dto.PhoneRegistrationRequest;
import com.example.food.auth.verification.PhoneVerificationCode;
import com.example.food.auth.verification.PhoneVerificationCodeMapper;
import com.example.food.auth.verification.VerificationCodeException;
import com.example.food.user.User;
import com.example.food.user.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class AuthServiceTest {

    private static final String TEST_PHONE_PREFIX = "13900002";

    @Autowired
    private AuthService authService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PhoneVerificationCodeMapper verificationCodeMapper;

    @BeforeEach
    void cleanTestData() {
        verificationCodeMapper.delete(new QueryWrapper<PhoneVerificationCode>()
                .likeRight("phone", TEST_PHONE_PREFIX));
        userMapper.delete(new QueryWrapper<User>().likeRight("phone", TEST_PHONE_PREFIX));
    }

    @Test
    void registerUserPersistsNicknameAndReturnsJwt() {
        String phone = "13900002001";
        String code = authService.issueRegistrationCode(phone).code();

        AuthResponse response = authService.registerUser(
                new PhoneRegistrationRequest(phone, code, "Recipe User")
        );

        User stored = userMapper.selectOne(new QueryWrapper<User>().eq("phone", phone));
        assertThat(stored).isNotNull();
        assertThat(stored.getNickname()).isEqualTo("Recipe User");
        assertThat(stored.getRole()).isEqualTo("USER");
        assertThat(stored.getEnabled()).isTrue();
        assertThat(stored.getLastLoginAt()).isNotNull();
        assertThat(response.id()).isEqualTo(stored.getId());
        assertThat(response.token()).isNotBlank();
    }

    @Test
    void existingPhoneCannotRequestRegistrationCode() {
        insertUser("13900002002", true);

        assertThatThrownBy(() -> authService.issueRegistrationCode("13900002002"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Phone already registered");
    }

    @Test
    void unknownPhoneCannotRequestLoginCode() {
        assertThatThrownBy(() -> authService.issueLoginCode("13900002003"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User not registered");
    }

    @Test
    void disabledUserCannotRequestLoginCode() {
        insertUser("13900002004", false);

        assertThatThrownBy(() -> authService.issueLoginCode("13900002004"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User account disabled");
    }

    @Test
    void registeredUserCanLoginWithIssuedCode() {
        User user = insertUser("13900002005", true);
        String code = authService.issueLoginCode(user.getPhone()).code();

        AuthResponse response = authService.loginUser(new PhoneLoginRequest(user.getPhone(), code));

        User stored = userMapper.selectById(user.getId());
        assertThat(response.id()).isEqualTo(user.getId());
        assertThat(response.token()).isNotBlank();
        assertThat(stored.getLastLoginAt()).isNotNull();
    }

    @Test
    void wrongLoginAttemptsPersistAcrossAuthenticationTransactions() {
        User user = insertUser("13900002006", true);
        String code = authService.issueLoginCode(user.getPhone()).code();
        String wrongCode = code.equals("000000") ? "999999" : "000000";

        for (int attempt = 0; attempt < 4; attempt++) {
            assertThatThrownBy(() -> authService.loginUser(
                    new PhoneLoginRequest(user.getPhone(), wrongCode)
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Verification code invalid");
        }
        assertThatThrownBy(() -> authService.loginUser(
                new PhoneLoginRequest(user.getPhone(), wrongCode)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Too many verification attempts");
        assertThatThrownBy(() -> authService.loginUser(
                new PhoneLoginRequest(user.getPhone(), code)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Too many verification attempts");
    }

    @Test
    void loginUserUsesReadCommittedTransactionIsolation() throws NoSuchMethodException {
        Method loginUser = AuthService.class.getMethod("loginUser", PhoneLoginRequest.class);

        Transactional transactional = loginUser.getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.isolation()).isEqualTo(Isolation.READ_COMMITTED);
        assertThat(transactional.noRollbackFor()).contains(VerificationCodeException.class);
    }

    @Test
    void registrationCommitsVerificationFailuresButRollsBackOtherErrors() throws NoSuchMethodException {
        Method registerUser = AuthService.class.getMethod(
                "registerUser",
                PhoneRegistrationRequest.class
        );

        Transactional transactional = registerUser.getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.noRollbackFor()).contains(VerificationCodeException.class);
    }

    private User insertUser(String phone, boolean enabled) {
        LocalDateTime now = LocalDateTime.now();
        User user = new User();
        user.setPhone(phone);
        user.setNickname(phone);
        user.setRole("USER");
        user.setEnabled(enabled);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userMapper.insert(user);
        return user;
    }
}
