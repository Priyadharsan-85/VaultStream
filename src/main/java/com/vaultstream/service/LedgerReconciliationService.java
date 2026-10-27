package com.vaultstream.service;

import com.vaultstream.dto.ReconciliationReportDto;
import com.vaultstream.model.Account;
import com.vaultstream.model.LedgerEntry;
import com.vaultstream.repository.AccountRepository;
import com.vaultstream.repository.LedgerEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LedgerReconciliationService {

    private static final UUID CLEARING_ACCOUNT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final LedgerEntryRepository ledgerEntryRepository;
    private final AccountRepository accountRepository;

    private volatile ReconciliationReportDto lastReport;

    @Transactional(readOnly = true)
    public ReconciliationReportDto reconcile() {
        LocalDateTime timestamp = LocalDateTime.now();
        BigDecimal totalDebits = ledgerEntryRepository.sumAmountByDirection(LedgerEntry.Direction.DEBIT);
        BigDecimal totalCredits = ledgerEntryRepository.sumAmountByDirection(LedgerEntry.Direction.CREDIT);
        BigDecimal systemImbalance = totalDebits.subtract(totalCredits).abs();
        boolean systemBalanced = systemImbalance.compareTo(BigDecimal.ZERO) == 0;

        List<Account> accounts = accountRepository.findAll();
        List<ReconciliationReportDto.AccountDiscrepancyDto> discrepancies = new ArrayList<>();
        int customerAccountsAudited = 0;

        for (Account account : accounts) {
            if (CLEARING_ACCOUNT_ID.equals(account.getId())) {
                continue;
            }
            customerAccountsAudited++;
            BigDecimal ledgerBalance = ledgerEntryRepository.calculateLedgerBalanceForAccount(account.getId());
            BigDecimal difference = account.getBalance().subtract(ledgerBalance);

            if (difference.compareTo(BigDecimal.ZERO) != 0) {
                discrepancies.add(ReconciliationReportDto.AccountDiscrepancyDto.builder()
                        .accountId(account.getId())
                        .accountNumber(account.getAccountNumber())
                        .accountBalance(account.getBalance())
                        .ledgerCalculatedBalance(ledgerBalance)
                        .difference(difference)
                        .build());
            }
        }

        ReconciliationReportDto report = ReconciliationReportDto.builder()
                .reconciliationTimestamp(timestamp)
                .totalDebits(totalDebits)
                .totalCredits(totalCredits)
                .systemImbalance(systemImbalance)
                .systemBalanced(systemBalanced && discrepancies.isEmpty())
                .accountsAudited(customerAccountsAudited)
                .discrepanciesCount(discrepancies.size())
                .discrepancies(discrepancies)
                .build();

        this.lastReport = report;

        if (!report.isSystemBalanced()) {
            log.warn("Ledger reconciliation found discrepancies: systemBalanced={}, accountDiscrepancies={}",
                    systemBalanced, discrepancies.size());
        } else {
            log.info("Ledger reconciliation completed successfully: {} accounts audited, all balanced",
                    customerAccountsAudited);
        }

        return report;
    }

    public ReconciliationReportDto getLastReport() {
        return lastReport;
    }

    @Transactional(readOnly = true)
    public boolean isSystemBalanced() {
        BigDecimal totalDebits = ledgerEntryRepository.sumAmountByDirection(LedgerEntry.Direction.DEBIT);
        BigDecimal totalCredits = ledgerEntryRepository.sumAmountByDirection(LedgerEntry.Direction.CREDIT);
        return totalDebits.compareTo(totalCredits) == 0;
    }

    @Scheduled(fixedDelayString = "${vaultstream.ledger.reconciliation-interval-ms:3600000}")
    public void runScheduledReconciliation() {
        reconcile();
    }
}
