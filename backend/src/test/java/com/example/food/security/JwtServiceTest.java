package com.example.food.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-32";

    @Test
    void generatesAndParsesUserToken() {
        JwtService service = new JwtService(SECRET, 1440);

        String token = service.generateToken(new AuthPrincipal(7L, "13800138000", AppRole.USER));
        AuthPrincipal principal = service.parseToken(token);

        assertThat(principal.id()).isEqualTo(7L);
        assertThat(principal.username()).isEqualTo("13800138000");
        assertThat(principal.role()).isEqualTo(AppRole.USER);
    }

    @Test
    void malformedTokenFailsWithAuthenticationException() {
        JwtService service = new JwtService(SECRET, 1440);

        assertThatThrownBy(() -> service.parseToken("not-a-jwt"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid JWT token");
    }

    @Test
    void missingIdFailsWithAuthenticationException() {
        JwtService service = new JwtService(SECRET, 1440);
        String token = signedToken(null, AppRole.USER.name());

        assertThatThrownBy(() -> service.parseToken(token))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid JWT token");
    }

    @Test
    void missingRoleFailsWithAuthenticationException() {
        JwtService service = new JwtService(SECRET, 1440);
        String token = signedToken(7L, null);

        assertThatThrownBy(() -> service.parseToken(token))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid JWT token");
    }

    @Test
    void unknownRoleFailsWithAuthenticationException() {
        JwtService service = new JwtService(SECRET, 1440);
        String token = signedToken(7L, "SUPERUSER");

        assertThatThrownBy(() -> service.parseToken(token))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid JWT token");
    }

    private String signedToken(Long id, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject("13800138000")
                .claim("id", id)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(3600)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
