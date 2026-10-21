package com.vaultstream.idempotency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyCleanupService {

    private final IdempotencyRepository idempotencyRepository;

    @Scheduled(fixedDelayString = "${vaultstream.idempotency.cleanup-interval-ms:3600000}")
    @Transactional
    public void deleteExpiredKeys() {
        int removed = idempotencyRepository.deleteExpired(LocalDateTime.now());
        if (removed > 0) {
            log.info("Removed {} expired idempotency records", removed);
        }
    }
}
