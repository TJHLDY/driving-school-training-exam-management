package com.example.drivingschool.security;

import com.example.drivingschool.config.JwtProperties;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(new JwtProperties(
            Duration.ofMinutes(30), "access_token", false, "Strict",
            "01234567890123456789012345678901"));

    @Test
    void createsAndValidatesToken() {
        LoginUser user = new LoginUser(42L, "student01", "hash", Set.of("STUDENT"), true);

        String token = jwtService.createToken(user);
        Claims claims = jwtService.parse(token).orElseThrow();

        assertThat(token).isNotBlank();
        assertThat(claims.getSubject()).isEqualTo("student01");
        assertThat(claims.get("uid", Long.class)).isEqualTo(42L);
        assertThat(jwtService.expiresInSeconds()).isEqualTo(1800L);
    }

    @Test
    void rejectsTamperedToken() {
        LoginUser user = new LoginUser(42L, "student01", "hash", Set.of("STUDENT"), true);
        String token = jwtService.createToken(user);

        assertThat(jwtService.parse(token + "x")).isEmpty();
    }
}
