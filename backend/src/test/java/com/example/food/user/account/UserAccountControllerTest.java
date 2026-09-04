package com.example.food.user.account;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.food.auth.verification.PhoneVerificationCode;
import com.example.food.auth.verification.PhoneVerificationCodeMapper;
import com.example.food.user.User;
import com.example.food.user.UserMapper;
import com.example.food.user.security.UserSecurityEvent;
import com.example.food.user.security.UserSecurityEventMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserAccountControllerTest {
    private static final String PHONE_PREFIX = "13900008";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PhoneVerificationCodeMapper verificationCodeMapper;

    @Autowired
    private UserSecurityEventMapper securityEventMapper;

    @BeforeEach
    void cleanTestData() {
        verificationCodeMapper.delete(new QueryWrapper<PhoneVerificationCode>().likeRight("phone", PHONE_PREFIX));
        userMapper.delete(new QueryWrapper<User>().likeRight("phone", PHONE_PREFIX));
    }

    @Test
    void userCanReadAndUpdateOwnAccountProfile() throws Exception {
        String phone = PHONE_PREFIX + "001";
        String token = register(phone, "账号用户");

        mockMvc.perform(get("/api/users/me/account").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phone").value(phone))
                .andExpect(jsonPath("$.data.nickname").value("账号用户"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        mockMvc.perform(patch("/api/users/me/account/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"新账号昵称\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("新账号昵称"));

        User stored = userMapper.selectOne(new QueryWrapper<User>().eq("phone", phone));
        assertThat(stored.getNickname()).isEqualTo("新账号昵称");
        assertThat(securityEventMapper.selectList(new QueryWrapper<UserSecurityEvent>().eq("user_id", stored.getId())))
                .extracting(UserSecurityEvent::getEventType)
                .contains("PROFILE_UPDATED");
    }

    @Test
    void logoutAllDevicesInvalidatesTheCurrentUserJwt() throws Exception {
        String token = register(PHONE_PREFIX + "002", "退出设备用户");

        mockMvc.perform(post("/api/users/me/account/logout-all")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/me/account")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void userCanUploadAndReadOwnAvatarWithAuthentication() throws Exception {
        String token = register(PHONE_PREFIX + "004", "头像用户");
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "avatar.jpg",
                MediaType.TEXT_PLAIN_VALUE,
                new byte[]{(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10}
        );

        try {
            mockMvc.perform(multipart("/api/users/me/account/avatar")
                            .file(image)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.avatarUrl").value("/api/users/me/account/avatar"));

            mockMvc.perform(get("/api/users/me/account/avatar")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.IMAGE_PNG));
        } finally {
            mockMvc.perform(delete("/api/users/me/account/avatar")
                    .header("Authorization", "Bearer " + token));
        }
    }

    @Test
    void cancellationDisablesAccountAndInvalidatesJwt() throws Exception {
        String phone = PHONE_PREFIX + "003";
        String token = register(phone, "待注销用户");
        User before = userMapper.selectOne(new QueryWrapper<User>().eq("phone", phone));
        String code = json(mockMvc.perform(post("/api/users/me/account/cancel/code")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()).path("data").path("code").asText();

        mockMvc.perform(post("/api/users/me/account/cancel")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"%s\",\"confirmed\":true}".formatted(code)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/me/account")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());

        User stored = userMapper.selectById(before.getId());
        assertThat(stored.getEnabled()).isFalse();
        assertThat(stored.getDeletedAt()).isNotNull();
        assertThat(stored.getPhone()).startsWith("deleted-");
        assertThat(securityEventMapper.selectList(new QueryWrapper<UserSecurityEvent>().eq("user_id", stored.getId())))
                .extracting(UserSecurityEvent::getEventType)
                .contains("ACCOUNT_CANCELLED");
    }

    private String register(String phone, String nickname) throws Exception {
        String code = json(mockMvc.perform(post("/api/auth/user/register/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"%s\"}".formatted(phone)))
                .andExpect(status().isOk())
                .andReturn()).path("data").path("code").asText();
        return json(mockMvc.perform(post("/api/auth/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"%s\",\"code\":\"%s\",\"nickname\":\"%s\",\"password\":\"Recipe123\"}".formatted(phone, code, nickname)))
                .andExpect(status().isOk())
                .andReturn()).path("data").path("token").asText();
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}
