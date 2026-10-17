package com.vaultstream.monitoring;

import com.vaultstream.repository.AccountRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
@RequiredArgsConstructor
public class AccountMetrics {

    private final AccountRepository accountRepository;
    private final MeterRegistry meterRegistry;

    private Counter accountCreatedCounter;
    private Counter accountUpdatedCounter;
    private Counter accountDeletedCounter;

    @PostConstruct
    public void init() {
        accountCreatedCounter = Counter.builder("vaultstream.accounts.created")
                .description("Number of accounts created")
                .register(meterRegistry);

        accountUpdatedCounter = Counter.builder("vaultstream.accounts.updated")
                .description("Number of accounts updated")
                .register(meterRegistry);

        accountDeletedCounter = Counter.builder("vaultstream.accounts.deleted")
                .description("Number of accounts deleted")
                .register(meterRegistry);
    }

    public void incrementAccountCreatedCount() {
        accountCreatedCounter.increment();
    }

    public void incrementAccountUpdatedCount() {
        accountUpdatedCounter.increment();
    }

    public void incrementAccountDeletedCount() {
        accountDeletedCounter.increment();
    }

    public long getTotalAccountCount() {
        return accountRepository.count();
    }
}