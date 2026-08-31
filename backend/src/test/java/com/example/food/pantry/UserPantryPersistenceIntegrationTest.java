package com.example.food.pantry;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class UserPantryPersistenceIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserPantryService service;

    private Long userId;

    @BeforeEach
    void setUp() {
        String phone = "139" + String.format("%08d", Math.abs(System.nanoTime() % 100_000_000));
        jdbcTemplate.update(
                "INSERT INTO users (phone, nickname) VALUES (?, ?)",
                phone,
                "pantry-consume-integration"
        );
        userId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE phone = ?",
                Long.class,
                phone
        );
    }

    @AfterEach
    void tearDown() {
        if (userId != null) {
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId);
        }
    }

    @Test
    void atomicallyDecrementsPersistedQuantityAndRejectsOverConsumption() {
        jdbcTemplate.update(
                "INSERT INTO user_pantry_items (user_id, ingredient_name, quantity, unit) VALUES (?, ?, ?, ?)",
                userId,
                "番茄",
                new BigDecimal("2.50"),
                "个"
        );
        Long itemId = jdbcTemplate.queryForObject(
                "SELECT id FROM user_pantry_items WHERE user_id = ? AND ingredient_name = ?",
                Long.class,
                userId,
                "番茄"
        );

        assertThat(service.consume(userId, itemId, new BigDecimal("1.25")).quantity())
                .isEqualByComparingTo("1.25");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT quantity FROM user_pantry_items WHERE id = ?",
                BigDecimal.class,
                itemId
        )).isEqualByComparingTo("1.25");

        assertThatThrownBy(() -> service.consume(userId, itemId, new BigDecimal("1.26")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("库存数量不足");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT quantity FROM user_pantry_items WHERE id = ?",
                BigDecimal.class,
                itemId
        )).isEqualByComparingTo("1.25");
    }
}
