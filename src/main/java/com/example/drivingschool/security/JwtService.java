package com.example.drivingschool.security;

import com.example.drivingschool.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        if (properties.secret() == null || properties.secret().isBlank()) {
            this.signingKey = Jwts.SIG.HS256.key().build();
        } else {
            byte[] bytes = properties.secret().getBytes(StandardCharsets.UTF_8);
            if (bytes.length < 32) {
                throw new IllegalStateException("JWT_SECRET 至少需要 32 字节");
            }
            this.signingKey = Keys.hmacShaKeyFor(bytes);
        }
    }

    public String createToken(LoginUser user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.accessTokenTtl());
        return Jwts.builder()
                .subject(user.username())
                .claim("uid", user.userId())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
    }

    public Optional<Claims> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(claims);
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    public long expiresInSeconds() {
        return properties.accessTokenTtl().toSeconds();
    }
}
