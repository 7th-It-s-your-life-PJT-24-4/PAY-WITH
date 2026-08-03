package com.paywith.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("JwtTokenProvider")
class JwtTokenProviderTest {

    private static final String SECRET =
        Base64.getEncoder().encodeToString("test-jwt-secret-key-for-unit-test-only-1234567890".getBytes());

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(SECRET, 60_000L, 1_000_000L);
    }

    @Test
    @DisplayName("정상 발급한 토큰은 VALID다")
    void resolveStatus_valid() {
        String token = jwtTokenProvider.createAccessToken(1L, "01012345678");

        assertThat(jwtTokenProvider.resolveStatus(token)).isEqualTo(JwtTokenProvider.TokenStatus.VALID);
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("만료된 토큰은 EXPIRED다")
    void resolveStatus_expired() throws InterruptedException {
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider(SECRET, 1L, 1_000_000L);
        String token = shortLivedProvider.createAccessToken(1L, "01012345678");
        Thread.sleep(20);

        assertThat(shortLivedProvider.resolveStatus(token)).isEqualTo(JwtTokenProvider.TokenStatus.EXPIRED);
        assertThat(shortLivedProvider.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("형식이 잘못된 토큰은 INVALID다")
    void resolveStatus_invalid() {
        assertThat(jwtTokenProvider.resolveStatus("not-a-jwt")).isEqualTo(JwtTokenProvider.TokenStatus.INVALID);
        assertThat(jwtTokenProvider.validateToken("not-a-jwt")).isFalse();
    }
}
