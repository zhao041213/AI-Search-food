package com.example.food.auth.verification;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!prod")
@ConditionalOnProperty(prefix = "app.sms", name = "provider", havingValue = "mock", matchIfMissing = true)
public class MockSmsCodeSender implements SmsCodeSender {

    @Override
    public SmsSendResult send(String phone, String code) {
        return new SmsSendResult(code, 0);
    }
}
