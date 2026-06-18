package com.example.food.ai.qwen;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local")
class QwenPropertiesLocalProfileTest {

    @Autowired
    private QwenProperties qwenProperties;

    @Test
    void localProfileBindsQwenApiKey() {
        assertThat(qwenProperties.apiKey()).isEqualTo("local-profile-test-key");
    }
}
