package com.example.food.config;

import com.example.food.ai.qwen.QwenProperties;
import com.example.food.auth.verification.SmsProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DockerConfigurationValidatorTest {

    @Test
    void missingDockerSecretsAreReportedByNameOnly() {
        QwenProperties qwen = new QwenProperties();
        SmsProperties sms = new SmsProperties();
        sms.setProvider("aliyun-pnvs");

        assertThatThrownBy(() -> new DockerConfigurationValidator(
                "",
                "jdbc:mysql://localhost:3306/ai_smart_recipe",
                "root",
                qwen,
                sms
        ).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET")
                .hasMessageContaining("DASHSCOPE_API_KEY")
                .hasMessageContaining("MYSQL_URL")
                .hasMessageNotContaining("secret-value")
                .hasMessageNotContaining("api-key-value");
    }
}
