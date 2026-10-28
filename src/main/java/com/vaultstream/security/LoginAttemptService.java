package com.vaultstream.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service to protect authentication endpoints against brute-force attacks and credential stuffing.
 * Tracks failed login attempts per username and temporarily locks accounts when the threshold is exceeded.
 */
@Service
@Slf4j
public class LoginAttemptService {

    @Value("${vaultstream.security.max-login-attempts:5}")
    private int maxAttempts = 5;

    @Value("${vaultstream.security.lockout-duration-minutes:15}")
    private int lockoutDurationMinutes = 15;

    private final ConcurrentMap<String, AttemptTracker> attemptsMap = new ConcurrentHashMap<>();

    public void loginSucceeded(String key) {
        if (key != null) {
            attemptsMap.remove(normalizeKey(key));
        }
    }

    public void loginFailed(String key) {
        if (key == null) {
            return;
        }
        String normalized = normalizeKey(key);
        attemptsMap.compute(normalized, (k, existing) -> {
            LocalDateTime now = LocalDateTime.now();
            if (existing == null) {
                return new AttemptTracker(1, now);
            }
            if (existing.getLastAttemptTime().plusMinutes(lockoutDurationMinutes).isBefore(now)) {
                // Previous lockout/tracking window has passed, reset counter
                return new AttemptTracker(1, now);
            }
            existing.incrementAttempts();
            existing.setLastAttemptTime(now);
            return existing;
        });
        log.warn("Failed login attempt recorded for key='{}'. Attempts: {}", normalized,
                attemptsMap.get(normalized) != null ? attemptsMap.get(normalized).getAttempts() : 1);
    }

    public boolean isBlocked(String key) {
        if (key == null) {
            return false;
        }
        String normalized = normalizeKey(key);
        AttemptTracker tracker = attemptsMap.get(normalized);
        if (tracker == null) {
            return false;
        }

        if (tracker.getAttempts() >= maxAttempts) {
            LocalDateTime expiresAt = tracker.getLastAttemptTime().plusMinutes(lockoutDurationMinutes);
            if (expiresAt.isAfter(LocalDateTime.now())) {
                log.warn("Login attempt blocked for '{}': exceeded {} failed attempts. Locked until {}",
                        normalized, maxAttempts, expiresAt);
                return true;
            } else {
                // Lockout duration has expired, remove entry
                attemptsMap.remove(normalized);
                return false;
            }
        }
        return false;
    }

    public int getRemainingAttempts(String key) {
        if (key == null) {
            return maxAttempts;
        }
        AttemptTracker tracker = attemptsMap.get(normalizeKey(key));
        if (tracker == null) {
            return maxAttempts;
        }
        return Math.max(0, maxAttempts - tracker.getAttempts());
    }

    public void reset(String key) {
        if (key != null) {
            attemptsMap.remove(normalizeKey(key));
        }
    }

    @Scheduled(fixedRate = 600000) // every 10 minutes
    public void cleanupExpiredTrackers() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(lockoutDurationMinutes);
        attemptsMap.entrySet().removeIf(entry -> entry.getValue().getLastAttemptTime().isBefore(cutoff));
    }

    private String normalizeKey(String key) {
        return key.trim().toLowerCase();
    }

    private static class AttemptTracker {
        private final AtomicInteger attempts;
        private volatile LocalDateTime lastAttemptTime;

        public AttemptTracker(int initialAttempts, LocalDateTime lastAttemptTime) {
            this.attempts = new AtomicInteger(initialAttempts);
            this.lastAttemptTime = lastAttemptTime;
        }

        public int getAttempts() {
            return attempts.get();
        }

        public void incrementAttempts() {
            attempts.incrementAndGet();
        }

        public LocalDateTime getLastAttemptTime() {
            return lastAttemptTime;
        }

        public void setLastAttemptTime(LocalDateTime lastAttemptTime) {
            this.lastAttemptTime = lastAttemptTime;
        }
    }
}
