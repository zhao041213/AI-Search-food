package com.example.food.auth.verification;

public record SmsSendResult(
        String code,
        long retryAfterSeconds
) {
}
