package com.deepanshu.taskmanager.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JwtTokenProvider Unit Tests")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    // Valid 256-bit Base64 key for testing
    private static final String TEST_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMs", 3600000L);
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshExpirationMs", 86400000L);
    }

    @Test
    @DisplayName("generateToken() - produces a non-blank JWT string")
    void generateToken_returnsNonBlankToken() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
            mockUserDetails("deepanshu"), null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        String token = jwtTokenProvider.generateToken(auth);
        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3); // header.payload.signature
    }

    @Test
    @DisplayName("extractUsername() - returns correct subject from token")
    void extractUsername_fromValidToken_returnsCorrectUsername() {
        String token = jwtTokenProvider.generateTokenFromUsername("deepanshu");
        String username = jwtTokenProvider.extractUsername(token);
        assertThat(username).isEqualTo("deepanshu");
    }

    @Test
    @DisplayName("validateToken() - valid token returns true")
    void validateToken_validToken_returnsTrue() {
        String token = jwtTokenProvider.generateTokenFromUsername("deepanshu");
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("validateToken() - tampered token returns false")
    void validateToken_tamperedToken_returnsFalse() {
        String token = jwtTokenProvider.generateTokenFromUsername("deepanshu");
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertThat(jwtTokenProvider.validateToken(tampered)).isFalse();
    }

    @Test
    @DisplayName("validateToken() - expired token returns false")
    void validateToken_expiredToken_returnsFalse() {
        // Set expiry to 1ms so it expires immediately
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMs", 1L);
        String token = jwtTokenProvider.generateTokenFromUsername("deepanshu");
        // Wait a tiny bit
        try { Thread.sleep(10); } catch (InterruptedException ignored) {}
        assertThat(jwtTokenProvider.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("generateRefreshToken() - produces separate token from access token")
    void generateRefreshToken_producesDistinctToken() {
        String access = jwtTokenProvider.generateTokenFromUsername("deepanshu");
        String refresh = jwtTokenProvider.generateRefreshToken("deepanshu");
        assertThat(access).isNotEqualTo(refresh);
        assertThat(jwtTokenProvider.extractUsername(refresh)).isEqualTo("deepanshu");
    }

    private org.springframework.security.core.userdetails.User mockUserDetails(String username) {
        return new org.springframework.security.core.userdetails.User(
            username, "password", List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}
