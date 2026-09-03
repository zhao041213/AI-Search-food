package com.example.food.auth;

public class PasswordLoginFailureException extends IllegalArgumentException {

    public PasswordLoginFailureException() {
        super("User phone or password invalid");
    }
}
