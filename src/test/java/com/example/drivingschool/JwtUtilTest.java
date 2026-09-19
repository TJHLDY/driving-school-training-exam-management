package com.example.drivingschool;

import com.example.drivingschool.config.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** JWT 工具验证：签发、解析、过期和防篡改 */
class JwtUtilTest {

    private static final String SECRET = "unit-test-jwt-secret-key-at-least-32-bytes-long";
    private static final Duration SEVEN_DAYS = Duration.ofDays(7);

    @Test
    @DisplayName("签发的令牌是三段结构，载荷能取回用户 ID 和登录名")
    void generateAndParse() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, SEVEN_DAYS);
        String token = jwtUtil.generate(15L, "241616118");

        // JWT 固定三段：头部.载荷.签名
        assertThat(token.split("\\.")).hasSize(3);

        Claims claims = jwtUtil.parse(token);
        assertThat(claims.getSubject()).isEqualTo("15");
        assertThat(claims.get("username")).isEqualTo("241616118");
        // 过期时间应该在一周后
        assertThat(claims.getExpiration()).isAfter(new Date(System.currentTimeMillis() + Duration.ofDays(6).toMillis()));
    }

    @Test
    @DisplayName("过期令牌解析失败，达到有效期自动失效")
    void expiredTokenRejected() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, Duration.ofMillis(-1000));
        String token = jwtUtil.generate(1L, "admin");

        assertThatThrownBy(() -> jwtUtil.parse(token)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("载荷被改动后签名对不上，解析失败")
    void tamperedTokenRejected() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, SEVEN_DAYS);
        String token = jwtUtil.generate(1L, "admin");

        String[] parts = token.split("\\.");
        String fakePayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"999\",\"username\":\"hacker\"}".getBytes(StandardCharsets.UTF_8));
        String tampered = parts[0] + "." + fakePayload + "." + parts[2];

        assertThatThrownBy(() -> jwtUtil.parse(tampered)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("换一把密钥就验不过，说明签名能防止伪造")
    void anotherSecretCannotVerify() {
        JwtUtil issuer = new JwtUtil(SECRET, SEVEN_DAYS);
        JwtUtil other = new JwtUtil("another-jwt-secret-key-with-at-least-32-bytes", SEVEN_DAYS);
        String token = issuer.generate(1L, "admin");

        assertThatThrownBy(() -> other.parse(token)).isInstanceOf(JwtException.class);
    }
}
