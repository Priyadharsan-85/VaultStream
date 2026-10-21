package com.vaultstream.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

    private static final String TEST_SECRET = "a".repeat(64);
    private static final String WRONG_SECRET = "b".repeat(64);
    private static final int TEST_EXPIRATION_MS = 3_600_000;

    @Mock
    private Authentication authentication;

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationInMs", TEST_EXPIRATION_MS);
    }

    @Test
    void generatesTokenWithUsernameAndExpectedExpiry() {
        when(authentication.getName()).thenReturn("testuser");
        long beforeGeneration = System.currentTimeMillis();

        String token = jwtTokenProvider.generateToken(authentication);

        Claims claims = Jwts.parser().verifyWith(signingKey(TEST_SECRET)).build()
                .parseSignedClaims(token).getPayload();
        assertEquals("testuser", claims.getSubject());
        assertNotNull(claims.getIssuedAt());
        assertTrue(claims.getExpiration().getTime() >= beforeGeneration + TEST_EXPIRATION_MS - 1_000);
        assertTrue(jwtTokenProvider.validateToken(token));
    }

    @Test
    void validatesTokenSignedWithConfiguredKey() {
        assertTrue(jwtTokenProvider.validateToken(createToken(TEST_SECRET,
                new Date(), new Date(System.currentTimeMillis() + TEST_EXPIRATION_MS))));
    }

    @Test
    void rejectsTokenWithDifferentSignature() {
        assertFalse(jwtTokenProvider.validateToken(createToken(WRONG_SECRET,
                new Date(), new Date(System.currentTimeMillis() + TEST_EXPIRATION_MS))));
    }

    @Test
    void rejectsExpiredToken() {
        long now = System.currentTimeMillis();
        assertFalse(jwtTokenProvider.validateToken(createToken(TEST_SECRET,
                new Date(now - 7_200_000), new Date(now - 3_600_000))));
    }

    @Test
    void rejectsMalformedToken() {
        assertFalse(jwtTokenProvider.validateToken("invalid.token.string"));
    }

    @Test
    void extractsUsernameFromSignedToken() {
        String token = createToken(TEST_SECRET, new Date(), new Date(System.currentTimeMillis() + TEST_EXPIRATION_MS));

        assertEquals("testuser", jwtTokenProvider.getUsernameFromToken(token));
    }

    private static String createToken(String secret, Date issuedAt, Date expiresAt) {
        return Jwts.builder()
                .subject("testuser")
                .issuedAt(issuedAt)
                .expiration(expiresAt)
                .signWith(signingKey(secret), SignatureAlgorithm.HS512)
                .compact();
    }

    private static SecretKey signingKey(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
