package com.vaultstream.service;

import com.vaultstream.dto.ReconciliationReportDto;
import com.vaultstream.model.Account;
import com.vaultstream.model.LedgerEntry;
import com.vaultstream.repository.AccountRepository;
import com.vaultstream.repository.LedgerEntryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LedgerReconciliationServiceTest {

    private static final UUID CLEARING_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private LedgerReconciliationService reconciliationService;

    @Test
    void reconcile_WhenLedgerIsFullyBalanced_ShouldReturnBalancedReport() {
        UUID account1Id = UUID.randomUUID();
        UUID account2Id = UUID.randomUUID();

        Account clearing = Account.builder().id(CLEARING_ID).balance(BigDecimal.ZERO).build();
        Account account1 = Account.builder().id(account1Id).accountNumber("ACCT1001").balance(new BigDecimal("500.00")).build();
        Account account2 = Account.builder().id(account2Id).accountNumber("ACCT1002").balance(new BigDecimal("300.00")).build();

        when(ledgerEntryRepository.sumAmountByDirection(LedgerEntry.Direction.DEBIT)).thenReturn(new BigDecimal("800.00"));
        when(ledgerEntryRepository.sumAmountByDirection(LedgerEntry.Direction.CREDIT)).thenReturn(new BigDecimal("800.00"));
        when(accountRepository.findAll()).thenReturn(List.of(clearing, account1, account2));
        when(ledgerEntryRepository.calculateLedgerBalanceForAccount(account1Id)).thenReturn(new BigDecimal("500.00"));
        when(ledgerEntryRepository.calculateLedgerBalanceForAccount(account2Id)).thenReturn(new BigDecimal("300.00"));

        ReconciliationReportDto report = reconciliationService.reconcile();

        assertNotNull(report);
        assertTrue(report.isSystemBalanced());
        assertEquals(0, report.getSystemImbalance().compareTo(BigDecimal.ZERO));
        assertEquals(new BigDecimal("800.00"), report.getTotalDebits());
        assertEquals(new BigDecimal("800.00"), report.getTotalCredits());
        assertEquals(2, report.getAccountsAudited());
        assertEquals(0, report.getDiscrepanciesCount());
        assertTrue(report.getDiscrepancies().isEmpty());
        assertSame(report, reconciliationService.getLastReport());
    }

    @Test
    void reconcile_WhenSystemDebitsAndCreditsDoNotMatch_ShouldReturnImbalancedReport() {
        when(ledgerEntryRepository.sumAmountByDirection(LedgerEntry.Direction.DEBIT)).thenReturn(new BigDecimal("1000.00"));
        when(ledgerEntryRepository.sumAmountByDirection(LedgerEntry.Direction.CREDIT)).thenReturn(new BigDecimal("900.00"));
        when(accountRepository.findAll()).thenReturn(List.of());

        ReconciliationReportDto report = reconciliationService.reconcile();

        assertNotNull(report);
        assertFalse(report.isSystemBalanced());
        assertEquals(new BigDecimal("100.00"), report.getSystemImbalance());
        assertEquals(0, report.getDiscrepanciesCount());
    }

    @Test
    void reconcile_WhenAccountBalanceDiffersFromLedgerEntries_ShouldReportDiscrepancy() {
        UUID accountId = UUID.randomUUID();
        Account account = Account.builder().id(accountId).accountNumber("ACCT2001").balance(new BigDecimal("600.00")).build();

        when(ledgerEntryRepository.sumAmountByDirection(LedgerEntry.Direction.DEBIT)).thenReturn(new BigDecimal("500.00"));
        when(ledgerEntryRepository.sumAmountByDirection(LedgerEntry.Direction.CREDIT)).thenReturn(new BigDecimal("500.00"));
        when(accountRepository.findAll()).thenReturn(List.of(account));
        when(ledgerEntryRepository.calculateLedgerBalanceForAccount(accountId)).thenReturn(new BigDecimal("500.00"));

        ReconciliationReportDto report = reconciliationService.reconcile();

        assertNotNull(report);
        assertFalse(report.isSystemBalanced());
        assertEquals(1, report.getDiscrepanciesCount());

        ReconciliationReportDto.AccountDiscrepancyDto discrepancy = report.getDiscrepancies().get(0);
        assertEquals(accountId, discrepancy.getAccountId());
        assertEquals("ACCT2001", discrepancy.getAccountNumber());
        assertEquals(new BigDecimal("600.00"), discrepancy.getAccountBalance());
        assertEquals(new BigDecimal("500.00"), discrepancy.getLedgerCalculatedBalance());
        assertEquals(new BigDecimal("100.00"), discrepancy.getDifference());
    }

    @Test
    void isSystemBalanced_ShouldReturnTrueWhenDebitsEqualCredits() {
        when(ledgerEntryRepository.sumAmountByDirection(LedgerEntry.Direction.DEBIT)).thenReturn(new BigDecimal("250.00"));
        when(ledgerEntryRepository.sumAmountByDirection(LedgerEntry.Direction.CREDIT)).thenReturn(new BigDecimal("250.00"));

        assertTrue(reconciliationService.isSystemBalanced());
    }

    @Test
    void isSystemBalanced_ShouldReturnFalseWhenDebitsDifferFromCredits() {
        when(ledgerEntryRepository.sumAmountByDirection(LedgerEntry.Direction.DEBIT)).thenReturn(new BigDecimal("250.00"));
        when(ledgerEntryRepository.sumAmountByDirection(LedgerEntry.Direction.CREDIT)).thenReturn(new BigDecimal("200.00"));

        assertFalse(reconciliationService.isSystemBalanced());
    }

    @Test
    void runScheduledReconciliation_ExecutesReconciliation() {
        when(ledgerEntryRepository.sumAmountByDirection(LedgerEntry.Direction.DEBIT)).thenReturn(BigDecimal.ZERO);
        when(ledgerEntryRepository.sumAmountByDirection(LedgerEntry.Direction.CREDIT)).thenReturn(BigDecimal.ZERO);
        when(accountRepository.findAll()).thenReturn(List.of());

        reconciliationService.runScheduledReconciliation();

        assertNotNull(reconciliationService.getLastReport());
    }
}
