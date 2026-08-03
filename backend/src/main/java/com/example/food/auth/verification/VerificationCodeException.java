package com.example.food.auth.verification;

public class VerificationCodeException extends IllegalArgumentException {

    public VerificationCodeException(String message) {
        super(message);
    }
}
