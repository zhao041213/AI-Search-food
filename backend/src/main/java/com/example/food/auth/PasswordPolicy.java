package com.example.food.auth;

import java.nio.charset.StandardCharsets;

public final class PasswordPolicy {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 64;
    private static final int MAX_BCRYPT_UTF8_BYTES = 72;

    private PasswordPolicy() {
    }

    public static void validate(String password) {
        if (password == null) {
            throw invalidPassword();
        }

        int codePointLength = password.codePointCount(0, password.length());
        boolean hasLetter = password.codePoints().anyMatch(codePoint ->
                (codePoint >= 'A' && codePoint <= 'Z') || (codePoint >= 'a' && codePoint <= 'z'));
        boolean hasDigit = password.codePoints().anyMatch(Character::isDigit);
        boolean withinUtf8Limit = password.getBytes(StandardCharsets.UTF_8).length <= MAX_BCRYPT_UTF8_BYTES;
        if (codePointLength < MIN_LENGTH
                || codePointLength > MAX_LENGTH
                || !hasLetter
                || !hasDigit
                || !withinUtf8Limit) {
            throw invalidPassword();
        }
    }

    private static IllegalArgumentException invalidPassword() {
        return new IllegalArgumentException("Password format invalid");
    }
}
