package com.bjj.tournament.security;

import com.bjj.tournament.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for JwtTokenProvider
 * Tests JWT token generation, validation, and extraction
 */
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private User testUser;
    private String validSecret;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();

        // Use a valid base64-encoded secret (256 bits)
        validSecret = "YmpqLXRvdXJuYW1lbnQtc2VjcmV0LWtleS1jaGFuZ2UtdGhpcy1pbi1wcm9kdWN0aW9uLW11c3QtYmUtYXQtbGVhc3QtMjU2LWJpdHM=";
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", validSecret);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMs", 86400000L); // 24 hours

        // Create test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setFullName("Test User");
        testUser.setRole("ROLE_USER");
        testUser.setEnabled(true);
        testUser.setAccountNonLocked(true);
    }

    @Test
    void generateToken_WithAuthentication_ReturnsValidToken() {
        // Given
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                testUser, null, testUser.getAuthorities()
        );

        // When
        String token = jwtTokenProvider.generateToken(authentication);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts
    }

    @Test
    void generateTokenFromUsername_ReturnsValidToken() {
        // When
        String token = jwtTokenProvider.generateTokenFromUsername("testuser");

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts
    }

    @Test
    void getUsernameFromToken_WithValidToken_ReturnsUsername() {
        // Given
        String token = jwtTokenProvider.generateTokenFromUsername("testuser");

        // When
        String username = jwtTokenProvider.getUsernameFromToken(token);

        // Then
        assertThat(username).isEqualTo("testuser");
    }

    @Test
    void validateToken_WithValidToken_ReturnsTrue() {
        // Given
        String token = jwtTokenProvider.generateTokenFromUsername("testuser");

        // When
        boolean isValid = jwtTokenProvider.validateToken(token);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    void validateToken_WithInvalidToken_ReturnsFalse() {
        // Given
        String invalidToken = "invalid.jwt.token";

        // When
        boolean isValid = jwtTokenProvider.validateToken(invalidToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void validateToken_WithMalformedToken_ReturnsFalse() {
        // Given
        String malformedToken = "malformed-token";

        // When
        boolean isValid = jwtTokenProvider.validateToken(malformedToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void validateToken_WithExpiredToken_ReturnsFalse() {
        // Given - create token with very short expiration
        JwtTokenProvider shortExpirationProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(shortExpirationProvider, "jwtSecret", validSecret);
        ReflectionTestUtils.setField(shortExpirationProvider, "jwtExpirationMs", -1000L); // Already expired

        String expiredToken = shortExpirationProvider.generateTokenFromUsername("testuser");

        // When
        boolean isValid = jwtTokenProvider.validateToken(expiredToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void validateToken_WithEmptyToken_ReturnsFalse() {
        // Given
        String emptyToken = "";

        // When
        boolean isValid = jwtTokenProvider.validateToken(emptyToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void validateToken_WithNullToken_ReturnsFalse() {
        // When/Then
        assertThatThrownBy(() -> jwtTokenProvider.validateToken(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void generateToken_CreatesTokenWithCorrectSubject() {
        // Given
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                testUser, null, testUser.getAuthorities()
        );

        // When
        String token = jwtTokenProvider.generateToken(authentication);
        String extractedUsername = jwtTokenProvider.getUsernameFromToken(token);

        // Then
        assertThat(extractedUsername).isEqualTo("testuser");
    }

    @Test
    void generateToken_DifferentUsersProduceDifferentTokens() {
        // Given
        String token1 = jwtTokenProvider.generateTokenFromUsername("user1");
        String token2 = jwtTokenProvider.generateTokenFromUsername("user2");

        // Then
        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    void getUsernameFromToken_WithDifferentUsers_ReturnsCorrectUsername() {
        // Given
        String token1 = jwtTokenProvider.generateTokenFromUsername("user1");
        String token2 = jwtTokenProvider.generateTokenFromUsername("user2");

        // When
        String username1 = jwtTokenProvider.getUsernameFromToken(token1);
        String username2 = jwtTokenProvider.getUsernameFromToken(token2);

        // Then
        assertThat(username1).isEqualTo("user1");
        assertThat(username2).isEqualTo("user2");
    }

    @Test
    void validateToken_WithTokenFromDifferentSecret_ReturnsFalse() {
        // Given - create token with different secret
        JwtTokenProvider differentSecretProvider = new JwtTokenProvider();
        String differentSecret = "ZGlmZmVyZW50LXNlY3JldC1rZXktdGhhdC1pcy1hdC1sZWFzdC0yNTYtYml0cy1sb25nLWZvci10ZXN0aW5nLXB1cnBvc2Vz";
        ReflectionTestUtils.setField(differentSecretProvider, "jwtSecret", differentSecret);
        ReflectionTestUtils.setField(differentSecretProvider, "jwtExpirationMs", 86400000L);

        String tokenWithDifferentSecret = differentSecretProvider.generateTokenFromUsername("testuser");

        // When
        boolean isValid = jwtTokenProvider.validateToken(tokenWithDifferentSecret);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void generateToken_CreatesUniqueTokensForSameUser() throws InterruptedException {
        // Given - tokens generated at different times
        String token1 = jwtTokenProvider.generateTokenFromUsername("testuser");
        Thread.sleep(10); // Small delay to ensure different issuedAt
        String token2 = jwtTokenProvider.generateTokenFromUsername("testuser");

        // Then - tokens should be different due to different issuedAt timestamps
        assertThat(token1).isNotEqualTo(token2);

        // But both should be valid and contain same username
        assertThat(jwtTokenProvider.validateToken(token1)).isTrue();
        assertThat(jwtTokenProvider.validateToken(token2)).isTrue();
        assertThat(jwtTokenProvider.getUsernameFromToken(token1)).isEqualTo("testuser");
        assertThat(jwtTokenProvider.getUsernameFromToken(token2)).isEqualTo("testuser");
    }

    @Test
    void validateToken_WithTamperedToken_ReturnsFalse() {
        // Given
        String validToken = jwtTokenProvider.generateTokenFromUsername("testuser");

        // Tamper with the token (change last character)
        String tamperedToken = validToken.substring(0, validToken.length() - 1) + "X";

        // When
        boolean isValid = jwtTokenProvider.validateToken(tamperedToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void validateToken_WithTokenMissingParts_ReturnsFalse() {
        // Given - token with only 2 parts instead of 3
        String incompleteToken = "header.payload";

        // When
        boolean isValid = jwtTokenProvider.validateToken(incompleteToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void generateToken_WithNullAuthentication_ThrowsException() {
        // When/Then
        assertThatThrownBy(() -> jwtTokenProvider.generateToken(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void generateTokenFromUsername_WithNullUsername_CreatesTokenWithNullSubject() {
        // When
        String token = jwtTokenProvider.generateTokenFromUsername(null);

        // Then
        assertThat(token).isNotNull();
        String extractedUsername = jwtTokenProvider.getUsernameFromToken(token);
        assertThat(extractedUsername).isNull();
    }
}
