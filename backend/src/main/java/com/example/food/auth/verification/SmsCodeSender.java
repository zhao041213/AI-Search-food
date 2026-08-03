package com.example.food.auth.verification;

public interface SmsCodeSender {

    SmsSendResult send(String phone, String code);
}
