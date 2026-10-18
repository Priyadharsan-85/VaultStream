package com.vaultstream.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

    @Mock
    private Authentication authentication;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private JwtTokenProvider jwtTokenProvider;

    private static final String TEST_SECRET = "test-secret-key-for-jwt-token-provider-testing";
    private static final int TEST_EXPIRATION_MS = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        // Set test values using reflection since fields are private
        jwtTokenProvider = new JwtTokenProvider();
        jwtTokenProvider.jwtSecret = TEST_SECRET;
        jwtTokenProvider.jwtExpirationInMs = TEST_EXPIRATION_MS;
    }

    @Test
    void generateToken_ShouldReturnValidJwtToken() {
        // Arrange
        when(authentication.getName()).thenReturn("testuser");

        // Act
        String token = jwtTokenProvider.generateToken(authentication);

        // Assert
        assertNotNull(token);
        assertFalse(token.isEmpty());

        // Verify token can be parsed and has expected claims
        Claims claims = Jwts.parser()
                .verifyWith(TEST_SECRET)
                .build()
                .parse(token)
                .getPayload();

        assertEquals("testuser", claims.getSubject());
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());

        // Verify expiration is approximately correct (within 1 second)
        long expirationInMs = claims.getExpiration().getTime();
        long expectedExpirationInMs = System.currentTimeMillis() + TEST_EXPIRATION_MS;
        assertTrue(Math.abs(expirationInMs - expectedExpirationInMs) < 1000);
    }

    @Test
    void validateToken_ShouldReturnTrue_ForValidToken() {
        // Arrange
        String validToken = Jwts.builder()
                .setSubject("testuser")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + TEST_EXPIRATION_MS))
                .signWith(SignatureAlgorithm.HS512, TEST_SECRET)
                .compact();

        // Act
        boolean isValid = jwtTokenProvider.validateToken(validToken);

        // Assert
        assertTrue(isValid);
    }

    @Test
    void validateToken_ShouldReturnFalse_ForInvalidSignature() {
        // Arrange
        String invalidToken = Jwts.builder()
                .setSubject("testuser")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + TEST_EXPIRATION_MS))
                .signWith(SignatureAlgorithm.HS512, "wrong-secret")
                .compact();

        // Act
        boolean isValid = jwtTokenProvider.validateToken(invalidToken);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void validateToken_ShouldReturnFalse_ForExpiredToken() {
        // Arrange
        String expiredToken = Jwts.builder()
                .setSubject("testuser")
                .setIssuedAt(new Date(System.currentTimeMillis() - 7200000)) // 2 hours ago
                .setExpiration(new Date(System.currentTimeMillis() - 3600000)) // 1 hour ago
                .signWith(SignatureAlgorithm.HS512, TEST_SECRET)
                .compact();

        // Act
        boolean isValid = jwtTokenProvider.validateToken(expiredToken);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void validateToken_ShouldReturnFalse_ForMalformedToken() {
        // Arrange
        String malformedToken = "invalid.token.string";

        // Act
        boolean isValid = jwtTokenProvider.validateToken(malformedToken);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void getUsernameFromToken_ShouldReturnCorrectUsername() {
        // Arrange
        String token = Jwts.builder()
                .setSubject("testuser")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + TEST_EXPIRATION_MS))
                .signWith(SignatureAlgorithm.HS512, TEST_SECRET)
                .compact();

        // Act
        String username = jwtTokenProvider.getUsernameFromToken(token);

        // Assert
        assertEquals("testuser", username);
    }
}