package com.example.food.user.character;

import com.example.food.user.character.dto.KitchenCharacterNamesRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserKitchenCharacterNamesPersistenceIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserKitchenCharacterNamesService service;

    private Long firstUserId;
    private Long secondUserId;

    @BeforeEach
    void setUp() {
        firstUserId = insertUser("人物名称用户一");
        secondUserId = insertUser("人物名称用户二");
    }

    @Test
    void persistsAndIsolatesNamesAndResetKeepsOtherUsersData() {
        service.save(firstUserId, request("第一主厨"));
        service.save(secondUserId, request("第二主厨"));

        assertThat(service.get(firstUserId).names().get("chef")).isEqualTo("第一主厨");
        assertThat(service.get(secondUserId).names().get("chef")).isEqualTo("第二主厨");

        service.reset(firstUserId);

        assertThat(service.get(firstUserId).hasCustomNames()).isFalse();
        assertThat(service.get(firstUserId).names().get("chef")).isEqualTo("阿灶");
        assertThat(service.get(secondUserId).names().get("chef")).isEqualTo("第二主厨");
    }

    private Long insertUser(String nickname) {
        String phone = "139" + String.format("%08d", Math.abs(System.nanoTime() % 100_000_000));
        jdbcTemplate.update("INSERT INTO users (phone, nickname) VALUES (?, ?)", phone, nickname);
        return jdbcTemplate.queryForObject("SELECT id FROM users WHERE phone = ?", Long.class, phone);
    }

    private KitchenCharacterNamesRequest request(String chefName) {
        Map<String, String> names = new LinkedHashMap<>();
        names.put("chef", chefName);
        names.put("chef-helper", "小灶");
        names.put("chef-recipes", "小谱");
        names.put("chef-nutrition", "小衡");
        names.put("pantry", "小仓");
        names.put("recipes", "阿笺");
        names.put("nutrition", "衡衡");
        names.put("weekly", "周周");
        names.put("review", "味味");
        names.put("account", "小管");
        names.put("hot", "椒椒");
        return new KitchenCharacterNamesRequest(names);
    }
}
