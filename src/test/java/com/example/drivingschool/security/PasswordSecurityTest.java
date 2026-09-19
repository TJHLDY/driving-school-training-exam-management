package com.example.drivingschool.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordSecurityTest {

    @Test
    void storesBCryptHashAndMatchesOnlyOriginalPassword() {
        PasswordEncoder encoder = new BCryptPasswordEncoder(12);
        String rawPassword = "Demo@123";
        String hash = encoder.encode(rawPassword);

        assertThat(hash).startsWith("$2");
        assertThat(hash).hasSize(60);
        assertThat(hash).doesNotContain(rawPassword);
        assertThat(encoder.matches(rawPassword, hash)).isTrue();
        assertThat(encoder.matches("Wrong@123", hash)).isFalse();
    }
}
