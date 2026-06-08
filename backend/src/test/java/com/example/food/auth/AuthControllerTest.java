package com.example.food.auth;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.food.admin.AdminAccount;
import com.example.food.admin.AdminMapper;
import com.example.food.user.User;
import com.example.food.user.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

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

    @BeforeEach
    void cleanTestAccounts() {
        userMapper.delete(new QueryWrapper<User>().likeRight("phone", TEST_PHONE_PREFIX));
        adminMapper.delete(new QueryWrapper<AdminAccount>().likeRight("username", TEST_ADMIN_PREFIX));
    }

    @Test
    void userCanRequestMockCode() throws Exception {
        mockMvc.perform(post("/api/auth/user/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"13900000001"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value("123456"));
    }

    @Test
    void userCanLoginWithMockCode() throws Exception {
        mockMvc.perform(post("/api/auth/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"13900000002","code":"123456"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.displayName").value("13900000002"))
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    @Test
    void userLoginWithWrongCodeReturnsBadRequestEnvelope() throws Exception {
        mockMvc.perform(post("/api/auth/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"13900000003","code":"000000"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void repeatedUserLoginReusesSamePhoneAccount() throws Exception {
        String body = """
                {"phone":"13900000004","code":"123456"}
                """;

        mockMvc.perform(post("/api/auth/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        User firstLoginUser = userMapper.selectOne(new QueryWrapper<User>().eq("phone", "13900000004"));
        assertThat(firstLoginUser).isNotNull();

        mockMvc.perform(post("/api/auth/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(firstLoginUser.getId()));

        Long userCount = userMapper.selectCount(new QueryWrapper<User>().eq("phone", "13900000004"));
        assertThat(userCount).isEqualTo(1L);
    }

    @Test
    void disabledUserCannotLogin() throws Exception {
        User user = new User();
        user.setPhone("13900000005");
        user.setNickname("disabled-user");
        user.setRole("USER");
        user.setEnabled(false);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);

        mockMvc.perform(post("/api/auth/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"13900000005","code":"123456"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
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
