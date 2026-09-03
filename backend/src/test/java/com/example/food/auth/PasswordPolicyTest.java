package com.example.food.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyTest {

    @Test
    void acceptsLettersDigitsAndSymbolsWithinLengthLimit() {
        assertThatCode(() -> PasswordPolicy.validate("Recipe#123"))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsPasswordsWithoutBothLettersAndDigits() {
        assertThatThrownBy(() -> PasswordPolicy.validate("recipe-only"))
                .hasMessage("Password format invalid");
        assertThatThrownBy(() -> PasswordPolicy.validate("12345678"))
                .hasMessage("Password format invalid");
    }

    @Test
    void rejectsPasswordsBeyondBcryptUtf8InputLimit() {
        assertThatThrownBy(() -> PasswordPolicy.validate("啊".repeat(24) + "A1"))
                .hasMessage("Password format invalid");
    }
}
