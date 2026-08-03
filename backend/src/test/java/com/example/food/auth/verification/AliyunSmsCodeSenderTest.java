package com.example.food.auth.verification;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AliyunSmsCodeSenderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void missingRequiredConfigurationIsRejected() {
        List<Consumer<SmsProperties.Aliyun>> missingConfigurations = List.of(
                config -> config.setAccessKeyId(""),
                config -> config.setAccessKeySecret(""),
                config -> config.setSignName(""),
                config -> config.setTemplateCode("")
        );

        for (Consumer<SmsProperties.Aliyun> removeRequiredValue : missingConfigurations) {
            SmsProperties properties = completeProperties();
            removeRequiredValue.accept(properties.getAliyun());

            assertThatThrownBy(() -> new AliyunSmsCodeSender(properties, objectMapper))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Aliyun SMS configuration incomplete");
        }
    }

    @Test
    void completeConfigurationCreatesSenderWithoutNetworkCall() {
        AliyunSmsCodeSender sender = new AliyunSmsCodeSender(completeProperties(), objectMapper);

        assertThat(sender).isNotNull();
    }

    private SmsProperties completeProperties() {
        SmsProperties properties = new SmsProperties();
        properties.setProvider("aliyun");
        properties.getAliyun().setAccessKeyId("test-access-key-id");
        properties.getAliyun().setAccessKeySecret("test-access-key-secret");
        properties.getAliyun().setSignName("test-sign");
        properties.getAliyun().setTemplateCode("SMS_123456789");
        return properties;
    }
}
