package com.example.food.auth.verification;

public interface SmsCodeSender {

    SmsSendResult send(String phone, String code);

    default boolean supportsRemoteVerification() {
        return false;
    }

    default void verify(String phone, String code) {
        throw new UnsupportedOperationException("Remote SMS verification is not supported");
    }
}
