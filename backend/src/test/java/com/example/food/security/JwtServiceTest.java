package com.example.food.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    @Test
    void generatesAndParsesUserToken() {
        JwtService service = new JwtService("test-secret-test-secret-test-secret-32", 1440);

        String token = service.generateToken(new AuthPrincipal(7L, "13800138000", AppRole.USER));
        AuthPrincipal principal = service.parseToken(token);

        assertThat(principal.id()).isEqualTo(7L);
        assertThat(principal.username()).isEqualTo("13800138000");
        assertThat(principal.role()).isEqualTo(AppRole.USER);
    }
}
