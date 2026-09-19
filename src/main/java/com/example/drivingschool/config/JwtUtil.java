package com.example.drivingschool.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

/**
 * JWT 工具：负责签发令牌和校验令牌。
 * 令牌只放身份信息（用户 ID、登录名），角色每次校验时重新查库，保证改角色后立即生效。
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final Duration expire;

    @Autowired
    public JwtUtil(@Value("${app.jwt.secret}") String secret,
                   @Value("${app.jwt.expire-days:7}") long expireDays) {
        this(secret, Duration.ofDays(expireDays));
    }

    /** 便于单元测试传入自定义有效期 */
    public JwtUtil(String secret, Duration expire) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expire = expire;
    }

    /** 签发令牌：subject 放用户 ID，附带登录名，签发时间和过期时间由这里写入 */
    public String generate(Long userId, String username) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expire.toMillis()))
                .signWith(key)
                .compact();
    }

    /**
     * 校验令牌并取出载荷。
     * 签名不对、被篡改或已过期都会抛 JwtException，调用方按“未登录”处理即可。
     */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getExpireMillis() {
        return expire.toMillis();
    }
}
