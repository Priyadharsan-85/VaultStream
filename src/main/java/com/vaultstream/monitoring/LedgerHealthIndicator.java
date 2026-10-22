package com.vaultstream.monitoring;

import com.vaultstream.repository.AccountRepository;
import com.vaultstream.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LedgerHealthIndicator implements HealthIndicator {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Override
    public Health health() {
        try {
            // Check if we can access the database
            long accountCount = accountRepository.count();
            long transactionCount = transactionRepository.count();

            return Health.up()
                    .withDetail("accounts", accountCount)
                    .withDetail("transactions", transactionCount)
                    .withDetail("ledger_status", "operational")
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withException(e)
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
