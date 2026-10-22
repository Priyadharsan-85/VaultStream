package com.vaultstream.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenRevocationService {

    private final RevokedTokenRepository revokedTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public void revoke(String token) {
        String tokenHash = hash(token);
        if (!revokedTokenRepository.existsByTokenHash(tokenHash)) {
            revokedTokenRepository.save(RevokedToken.builder()
                    .tokenHash(tokenHash)
                    .expiresAt(jwtTokenProvider.getExpirationFromToken(token).toInstant())
                    .build());
        }
    }

    @Transactional(readOnly = true)
    public boolean isRevoked(String token) {
        return revokedTokenRepository.existsByTokenHash(hash(token));
    }

    @Scheduled(fixedDelayString = "${vaultstream.security.revocation-cleanup-interval-ms:3600000}")
    @Transactional
    public void deleteExpiredRevocations() {
        int removed = revokedTokenRepository.deleteExpired(Instant.now());
        if (removed > 0) {
            log.info("Removed {} expired revoked-token records", removed);
        }
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }
}
