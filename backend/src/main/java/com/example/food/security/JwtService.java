package com.example.food.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expirationMinutes;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-minutes}") long expirationMinutes
    ) {
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 bytes");
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    public String generateToken(AuthPrincipal principal) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(expirationMinutes * 60);
        return Jwts.builder()
                .subject(principal.username())
                .claim("id", principal.id())
                .claim("role", principal.role().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact();
    }

    public AuthPrincipal parseToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw invalidToken();
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return toPrincipal(claims);
        } catch (JwtException | IllegalArgumentException exception) {
            throw invalidToken(exception);
        }
    }

    private AuthPrincipal toPrincipal(Claims claims) {
        Number id = claims.get("id", Number.class);
        String username = claims.getSubject();
        String role = claims.get("role", String.class);

        if (id == null || !StringUtils.hasText(username) || !StringUtils.hasText(role)) {
            throw invalidToken();
        }

        return new AuthPrincipal(id.longValue(), username, AppRole.valueOf(role));
    }

    private BadCredentialsException invalidToken() {
        return new BadCredentialsException("Invalid JWT token");
    }

    private BadCredentialsException invalidToken(Exception exception) {
        return new BadCredentialsException("Invalid JWT token", exception);
    }
}
