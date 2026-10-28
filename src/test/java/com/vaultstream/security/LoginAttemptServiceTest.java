package com.vaultstream.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class LoginAttemptServiceTest {

    private LoginAttemptService loginAttemptService;

    @BeforeEach
    void setUp() {
        loginAttemptService = new LoginAttemptService();
        ReflectionTestUtils.setField(loginAttemptService, "maxAttempts", 5);
        ReflectionTestUtils.setField(loginAttemptService, "lockoutDurationMinutes", 15);
    }

    @Test
    void isBlocked_WhenNoAttempts_ReturnsFalse() {
        assertFalse(loginAttemptService.isBlocked("alice"));
        assertEquals(5, loginAttemptService.getRemainingAttempts("alice"));
    }

    @Test
    void isBlocked_WhenNullKey_ReturnsFalse() {
        assertFalse(loginAttemptService.isBlocked(null));
        assertEquals(5, loginAttemptService.getRemainingAttempts(null));
    }

    @Test
    void loginFailed_IncrementsAttempts_AndBlocksWhenThresholdReached() {
        String username = "bob";
        for (int i = 1; i <= 4; i++) {
            loginAttemptService.loginFailed(username);
            assertFalse(loginAttemptService.isBlocked(username));
            assertEquals(5 - i, loginAttemptService.getRemainingAttempts(username));
        }

        loginAttemptService.loginFailed(username);
        assertTrue(loginAttemptService.isBlocked(username));
        assertEquals(0, loginAttemptService.getRemainingAttempts(username));
    }

    @Test
    void loginSucceeded_ClearsAttempts() {
        String username = "carol";
        loginAttemptService.loginFailed(username);
        loginAttemptService.loginFailed(username);
        assertEquals(3, loginAttemptService.getRemainingAttempts(username));

        loginAttemptService.loginSucceeded(username);

        assertFalse(loginAttemptService.isBlocked(username));
        assertEquals(5, loginAttemptService.getRemainingAttempts(username));
    }

    @Test
    void reset_ClearsBlockedState() {
        String username = "dave";
        for (int i = 0; i < 5; i++) {
            loginAttemptService.loginFailed(username);
        }
        assertTrue(loginAttemptService.isBlocked(username));

        loginAttemptService.reset(username);

        assertFalse(loginAttemptService.isBlocked(username));
        assertEquals(5, loginAttemptService.getRemainingAttempts(username));
    }

    @Test
    void isBlocked_NormalizesKeyCaseAndWhitespace() {
        for (int i = 0; i < 5; i++) {
            loginAttemptService.loginFailed("  EveUser  ");
        }

        assertTrue(loginAttemptService.isBlocked("eveuser"));
        assertTrue(loginAttemptService.isBlocked("EVEUSER"));
        assertTrue(loginAttemptService.isBlocked("  eveuser  "));
    }

    @Test
    void cleanupExpiredTrackers_ExecutesWithoutError() {
        loginAttemptService.loginFailed("frank");
        assertDoesNotThrow(() -> loginAttemptService.cleanupExpiredTrackers());
    }
}
