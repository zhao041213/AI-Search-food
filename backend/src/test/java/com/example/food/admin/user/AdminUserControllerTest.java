package com.example.food.admin.user;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.food.admin.operation.AdminOperationLog;
import com.example.food.admin.operation.AdminOperationLogMapper;
import com.example.food.admin.operation.AdminOperationLogService;
import com.example.food.auth.AuthService;
import com.example.food.auth.dto.PhoneLoginRequest;
import com.example.food.security.AppRole;
import com.example.food.security.AuthPrincipal;
import com.example.food.security.JwtService;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminUserControllerTest {

    private static final String TEST_PHONE_PREFIX = "13900007";
    private static final String PASSWORD = "Recipe123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AdminOperationLogMapper operationLogMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthService authService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanUsers() {
        userMapper.delete(new QueryWrapper<User>().likeRight("phone", TEST_PHONE_PREFIX));
    }

    @Test
    void adminCanSearchFilterPaginateAndSeeOnlySafeUserFields() throws Exception {
        User locked = insertUser("13900007001", "小明", true);
        locked.setPasswordLockedUntil(LocalDateTime.now().plusMinutes(15));
        locked.setPasswordFailedAttempts(5);
        userMapper.updatePasswordState(locked);
        insertUser("13900007002", "小红", false);

        mockMvc.perform(get("/api/admin/users")
                        .param("keyword", "小明")
                        .param("enabled", "true")
                        .param("locked", "true")
                        .param("limit", "500")
                        .param("offset", "-20")
                        .header("Authorization", adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.limit").value(100))
                .andExpect(jsonPath("$.data.offset").value(0))
                .andExpect(jsonPath("$.data.items[0].id").value(locked.getId()))
                .andExpect(jsonPath("$.data.items[0].phone").value("139****7001"))
                .andExpect(jsonPath("$.data.items[0].passwordLocked").value(true))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("passwordHash"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("authVersion"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("passwordLockedUntil"))));
    }

    @Test
    void anonymousAndNormalUserCannotManageUsers() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + jwtService.generateToken(
                                new AuthPrincipal(7L, "13800138000", AppRole.USER))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void disablingInvalidatesOldJwtAndEnablingAllowsOnlyNewLogin() throws Exception {
        User user = insertUser("13900007003", "状态用户", true);
        user.setAuthVersion(0);
        userMapper.updateById(user);
        String oldToken = jwtService.generateToken(new AuthPrincipal(user.getId(), user.getPhone(), AppRole.USER, 0L));

        mockMvc.perform(put("/api/admin/users/{id}/status", user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false,\"reason\":\"违反社区规则\"}")
                        .header("Authorization", adminToken())
                        .header("X-Forwarded-For", "10.0.0.8, 10.0.0.9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));

        User disabled = userMapper.selectById(user.getId());
        assertThat(disabled.getAuthVersion()).isEqualTo(1);
        assertThat(disabled.getLastLoginAt()).isNotNull();

        mockMvc.perform(get("/api/users/me/account")
                        .header("Authorization", "Bearer " + oldToken))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/user/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13900007003\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User account disabled"));
        mockMvc.perform(post("/api/auth/user/password-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13900007003\",\"password\":\"Recipe123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User account disabled"));

        mockMvc.perform(put("/api/admin/users/{id}/status", user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true,\"reason\":\"复核后恢复\"}")
                        .header("Authorization", adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true));

        User enabled = userMapper.selectById(user.getId());
        assertThat(enabled.getAuthVersion()).isEqualTo(1);
        mockMvc.perform(get("/api/users/me/account")
                        .header("Authorization", "Bearer " + oldToken))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/user/password-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13900007003\",\"password\":\"Recipe123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty());
        assertThat(userMapper.selectById(user.getId()).getLastLoginAt()).isNotNull();

        String code = authService.issueLoginCode(user.getPhone()).code();
        assertThat(authService.loginUser(new PhoneLoginRequest(user.getPhone(), code)).id())
                .isEqualTo(user.getId());
        assertThat(userMapper.selectById(user.getId()).getLastLoginAt()).isNotNull();

        List<AdminOperationLog> logs = operationLogMapper.selectList(new QueryWrapper<AdminOperationLog>()
                .eq("operation_type", AdminOperationLogService.USER_DISABLE)
                .like("request_path", "/api/admin/users/" + user.getId() + "/status"));
        assertThat(logs).anySatisfy(log -> {
            assertThat(log.getOperationResult()).isEqualTo(AdminOperationLogService.SUCCESS);
            assertThat(log.getStatusCode()).isEqualTo(200);
            assertThat(log.getIpAddress()).isEqualTo("10.0.0.8");
            assertThat(log.getDetails()).contains("targetUserId=" + user.getId(), "phone=139****7003", "违反社区规则");
            assertThat(log.getDetails()).doesNotContain(user.getPhone());
        });
    }

    @Test
    void invalidDisableReasonIsRejectedAndFailureIsAudited() throws Exception {
        User user = insertUser("13900007004", "原因用户", true);

        mockMvc.perform(put("/api/admin/users/{id}/status", user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false,\"reason\":\"   \"}")
                        .header("Authorization", adminToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("禁用原因不能为空"));

        assertThat(userMapper.selectById(user.getId()).getEnabled()).isTrue();
        assertThat(operationLogMapper.selectList(new QueryWrapper<AdminOperationLog>()
                .eq("operation_type", AdminOperationLogService.USER_DISABLE)
                .eq("operation_result", AdminOperationLogService.FAILURE)
                .like("request_path", "/api/admin/users/" + user.getId() + "/status")))
                .isNotEmpty();
    }

    @Test
    void clearingLockResetsOnlyPasswordLockFieldsAndKeepsDisabledAccountDisabled() throws Exception {
        User user = insertUser("13900007005", "锁定用户", true);
        user.setAuthVersion(6);
        user.setPasswordFailedAttempts(5);
        user.setPasswordLockedUntil(LocalDateTime.now().plusMinutes(15));
        String passwordHash = user.getPasswordHash();
        userMapper.updatePasswordState(user);

        mockMvc.perform(post("/api/admin/users/{id}/password-lock/clear", user.getId())
                        .header("Authorization", adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.passwordLocked").value(false))
                .andExpect(jsonPath("$.data.enabled").value(true));

        User cleared = userMapper.selectById(user.getId());
        assertThat(cleared.getPasswordFailedAttempts()).isZero();
        assertThat(cleared.getPasswordLockedUntil()).isNull();
        assertThat(cleared.getPasswordHash()).isEqualTo(passwordHash);
        assertThat(cleared.getAuthVersion()).isEqualTo(6);

        mockMvc.perform(post("/api/admin/users/{id}/password-lock/clear", user.getId())
                        .header("Authorization", adminToken()))
                .andExpect(status().isOk());

        User disabled = insertUser("13900007006", "禁用锁定用户", false);
        disabled.setAuthVersion(8);
        disabled.setPasswordFailedAttempts(5);
        disabled.setPasswordLockedUntil(LocalDateTime.now().plusMinutes(15));
        userMapper.updatePasswordState(disabled);
        mockMvc.perform(post("/api/admin/users/{id}/password-lock/clear", disabled.getId())
                        .header("Authorization", adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));
        assertThat(userMapper.selectById(disabled.getId()).getAuthVersion()).isEqualTo(8);
        mockMvc.perform(post("/api/auth/user/password-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13900007006\",\"password\":\"Recipe123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User account disabled"));
    }

    private User insertUser(String phone, String nickname, boolean enabled) {
        LocalDateTime now = LocalDateTime.now().minusDays(1);
        User user = new User();
        user.setPhone(phone);
        user.setNickname(nickname);
        user.setRole("USER");
        user.setEnabled(enabled);
        user.setAuthVersion(0);
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setPasswordFailedAttempts(0);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.setLastLoginAt(now);
        userMapper.insert(user);
        return user;
    }

    private String adminToken() {
        return "Bearer " + jwtService.generateToken(new AuthPrincipal(1L, "admin", AppRole.ADMIN));
    }
}
