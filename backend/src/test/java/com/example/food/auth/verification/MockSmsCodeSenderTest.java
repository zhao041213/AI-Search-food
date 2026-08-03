package com.example.food.auth.verification;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;

import static org.assertj.core.api.Assertions.assertThat;

class MockSmsCodeSenderTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(MockSmsCodeSender.class)
            .withPropertyValues("app.sms.provider=mock");

    @Test
    void mockSenderIsAvailableOutsideProduction() {
        contextRunner.run(context ->
                assertThat(context).hasSingleBean(SmsCodeSender.class));
    }

    @Test
    void mockSenderIsDisabledInProduction() {
        contextRunner
                .withInitializer(context -> context.getEnvironment().setActiveProfiles("prod"))
                .run(context -> assertThat(context).doesNotHaveBean(SmsCodeSender.class));
    }

    @Test
    void productionProfileDefaultsToAliyunAndDisablesMockSender() {
        new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withPropertyValues("spring.profiles.active=prod")
                .withUserConfiguration(MockSmsCodeSender.class)
                .run(context -> {
                    assertThat(context.getEnvironment().getProperty("app.sms.provider"))
                            .isEqualTo("aliyun");
                    assertThat(context).doesNotHaveBean(SmsCodeSender.class);
                });
    }
}
