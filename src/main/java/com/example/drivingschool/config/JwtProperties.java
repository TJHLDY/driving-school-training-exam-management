package com.example.drivingschool.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        Duration accessTokenTtl,
        String cookieName,
        boolean cookieSecure,
        String cookieSameSite,
        String secret
) {
    public JwtProperties {
        if (accessTokenTtl == null) {
            accessTokenTtl = Duration.ofHours(2);
        }
        if (cookieName == null || cookieName.isBlank()) {
            cookieName = "access_token";
        }
        if (cookieSameSite == null || cookieSameSite.isBlank()) {
            cookieSameSite = "Strict";
        }
    }
}
