package com.vaultstream.monitoring;

import com.vaultstream.model.LedgerEntry;
import com.vaultstream.repository.AccountRepository;
import com.vaultstream.repository.LedgerEntryRepository;
import com.vaultstream.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class LedgerHealthIndicator implements HealthIndicator {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    @Override
    public Health health() {
        try {
            long accountCount = accountRepository.count();
            long transactionCount = transactionRepository.count();
            BigDecimal totalDebits = ledgerEntryRepository.sumAmountByDirection(LedgerEntry.Direction.DEBIT);
            BigDecimal totalCredits = ledgerEntryRepository.sumAmountByDirection(LedgerEntry.Direction.CREDIT);
            boolean isBalanced = totalDebits.compareTo(totalCredits) == 0;

            Health.Builder builder = isBalanced ? Health.up() : Health.down();
            return builder
                    .withDetail("accounts", accountCount)
                    .withDetail("transactions", transactionCount)
                    .withDetail("total_debits", totalDebits)
                    .withDetail("total_credits", totalCredits)
                    .withDetail("ledger_balanced", isBalanced)
                    .withDetail("ledger_status", isBalanced ? "operational" : "imbalance_detected")
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withException(e)
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
