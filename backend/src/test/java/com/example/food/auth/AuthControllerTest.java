package com.example.food.auth;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.food.admin.AdminAccount;
import com.example.food.admin.AdminMapper;
import com.example.food.auth.verification.PhoneVerificationCode;
import com.example.food.auth.verification.PhoneVerificationCodeMapper;
import com.example.food.user.User;
import com.example.food.user.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    private static final String TEST_PHONE_PREFIX = "1390000";
    private static final String TEST_ADMIN_PREFIX = "auth_test_";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AdminMapper adminMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PhoneVerificationCodeMapper verificationCodeMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanTestAccounts() {
        verificationCodeMapper.delete(new QueryWrapper<PhoneVerificationCode>()
                .likeRight("phone", TEST_PHONE_PREFIX));
        userMapper.delete(new QueryWrapper<User>().likeRight("phone", TEST_PHONE_PREFIX));
        adminMapper.delete(new QueryWrapper<AdminAccount>().likeRight("username", TEST_ADMIN_PREFIX));
    }

    @Test
    void userCanRequestRegistrationCode() throws Exception {
        mockMvc.perform(post("/api/auth/user/register/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"13900000001"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.code").value(org.hamcrest.Matchers.matchesPattern("\\d{6}")))
                .andExpect(jsonPath("$.data.retryAfterSeconds").value(60));
    }

    @Test
    void userCanRegisterAndDataIsSaved() throws Exception {
        String phone = "13900000002";
        String code = requestRegistrationCode(phone);

        mockMvc.perform(post("/api/auth/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"%s","code":"%s","nickname":"注册用户"}
                                """.formatted(phone, code)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.displayName").value("注册用户"))
                .andExpect(jsonPath("$.data.token").isNotEmpty());

        User stored = userMapper.selectOne(new QueryWrapper<User>().eq("phone", phone));
        assertThat(stored).isNotNull();
        assertThat(stored.getNickname()).isEqualTo("注册用户");
    }

    @Test
    void duplicatePhoneCannotRegisterAgain() throws Exception {
        String phone = "13900000003";
        register(phone, "首个用户");

        mockMvc.perform(post("/api/auth/user/register/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"%s\"}".formatted(phone)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Phone already registered"));
    }

    @Test
    void registrationRejectsInvalidPhone() throws Exception {
        mockMvc.perform(post("/api/auth/user/register/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"12345"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Invalid request parameters"));
    }

    @Test
    void registrationRejectsBlankNickname() throws Exception {
        String phone = "13900000004";
        String code = requestRegistrationCode(phone);

        mockMvc.perform(post("/api/auth/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"%s","code":"%s","nickname":" "}
                                """.formatted(phone, code)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid request parameters"));
    }

    @Test
    void unknownPhoneCannotRequestLoginCode() throws Exception {
        mockMvc.perform(post("/api/auth/user/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"13900000005"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User not registered"));
    }

    @Test
    void registeredUserCanLoginWithIssuedCode() throws Exception {
        String phone = "13900000006";
        register(phone, "登录用户");
        String code = requestLoginCode(phone);

        mockMvc.perform(post("/api/auth/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"%s","code":"%s"}
                                """.formatted(phone, code)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayName").value("登录用户"))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    @Test
    void wrongLoginCodeReturnsBadRequestEnvelope() throws Exception {
        String phone = "13900000007";
        register(phone, "验证码用户");
        String code = requestLoginCode(phone);
        String wrongCode = code.equals("000000") ? "999999" : "000000";

        mockMvc.perform(post("/api/auth/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"%s","code":"%s"}
                                """.formatted(phone, wrongCode)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Verification code invalid"));
    }

    @Test
    void disabledUserCannotRequestLoginCode() throws Exception {
        User user = new User();
        user.setPhone("13900000008");
        user.setNickname("disabled-user");
        user.setRole("USER");
        user.setEnabled(false);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);

        mockMvc.perform(post("/api/auth/user/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"13900000008"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User account disabled"));
    }

    @Test
    void adminCanLoginWithEnabledTestAccount() throws Exception {
        insertAdmin("auth_test_enabled", "secret-password", true);

        mockMvc.perform(post("/api/auth/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"auth_test_enabled","password":"secret-password"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.role").value("ADMIN"))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.displayName").value("Auth Test Admin"))
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    @Test
    void seedAdminCanLoginWithInitialPassword() throws Exception {
        mockMvc.perform(post("/api/auth/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"Admin@123456"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.role").value("ADMIN"))
                .andExpect(jsonPath("$.data.displayName").value("Administrator"))
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    @Test
    void seedAdminCannotLoginWithDefaultPassword() throws Exception {
        mockMvc.perform(post("/api/auth/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"admin123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Invalid admin credentials"));
    }

    @Test
    void wrongAdminPasswordReturnsGenericBadRequestEnvelope() throws Exception {
        insertAdmin("auth_test_wrong_password", "secret-password", true);

        mockMvc.perform(post("/api/auth/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"auth_test_wrong_password","password":"bad-password"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Invalid admin credentials"));
    }

    @Test
    void disabledAdminReturnsGenericBadRequestEnvelope() throws Exception {
        insertAdmin("auth_test_disabled", "secret-password", false);

        mockMvc.perform(post("/api/auth/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"auth_test_disabled","password":"secret-password"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Invalid admin credentials"));
    }

    @Test
    void unknownAdminReturnsGenericBadRequestEnvelope() throws Exception {
        mockMvc.perform(post("/api/auth/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"auth_test_missing","password":"secret-password"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Invalid admin credentials"));
    }

    private String requestRegistrationCode(String phone) throws Exception {
        String body = mockMvc.perform(post("/api/auth/user/register/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"%s\"}".formatted(phone)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(body).path("data").path("code").asText();
    }

    private String requestLoginCode(String phone) throws Exception {
        String body = mockMvc.perform(post("/api/auth/user/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"%s\"}".formatted(phone)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(body).path("data").path("code").asText();
    }

    private void register(String phone, String nickname) throws Exception {
        String code = requestRegistrationCode(phone);
        mockMvc.perform(post("/api/auth/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"%s","code":"%s","nickname":"%s"}
                                """.formatted(phone, code, nickname)))
                .andExpect(status().isOk());
    }

    private void insertAdmin(String username, String password, boolean enabled) {
        AdminAccount admin = new AdminAccount();
        admin.setUsername(username);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setNickname("Auth Test Admin");
        admin.setRole("ADMIN");
        admin.setEnabled(enabled);
        admin.setCreatedAt(LocalDateTime.now());
        admin.setUpdatedAt(LocalDateTime.now());
        adminMapper.insert(admin);
    }
}
