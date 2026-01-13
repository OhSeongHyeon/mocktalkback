package com.mocktalkback.global.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class Argon2PasswordEncoderTest {

    @Test
    // Argon2 인코더가 비밀번호를 정상적으로 해시하고 매칭하는지 테스트한다.
    void shouldEncodeAndMatchPassword() {
        // given
        PasswordEncoder encoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
        String rawPassword = "123123123";

        // when
        String encoded = encoder.encode(rawPassword);
        log.info("encoded: {}", encoded);
        boolean matches = encoder.matches(rawPassword, encoded);

        // then
        assertThat(encoded).isNotBlank();
        assertThat(matches).isTrue();
    }
}
