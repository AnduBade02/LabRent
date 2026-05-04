package ro.atemustard.labrent.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider(
                "test-secret-key-for-junit-only-must-be-long-enough-for-hmac-sha-256!!",
                3_600_000L);
    }

    @Test
    void roundTripsUsernameAndRole() {
        String token = provider.generateToken("alice", "USER");
        assertThat(provider.validateToken(token)).isTrue();
        assertThat(provider.getUsernameFromToken(token)).isEqualTo("alice");
        assertThat(provider.getRoleFromToken(token)).isEqualTo("USER");
    }

    @Test
    void invalidToken_failsValidation() {
        assertThat(provider.validateToken("not-a-jwt")).isFalse();
    }

    @Test
    void tokenSignedWithDifferentSecret_fails() {
        String token = provider.generateToken("alice", "ADMIN");
        JwtTokenProvider other = new JwtTokenProvider(
                "totally-different-secret-key-for-hmac-sha-256-also-long-enough!!",
                3_600_000L);
        assertThat(other.validateToken(token)).isFalse();
    }

    @Test
    void expiredToken_failsValidation() throws InterruptedException {
        JwtTokenProvider shortLived = new JwtTokenProvider(
                "test-secret-key-for-junit-only-must-be-long-enough-for-hmac-sha-256!!",
                1L);
        String token = shortLived.generateToken("alice", "USER");
        Thread.sleep(50);
        assertThat(shortLived.validateToken(token)).isFalse();
    }
}
