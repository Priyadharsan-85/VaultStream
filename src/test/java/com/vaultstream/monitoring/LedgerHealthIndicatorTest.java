package com.vaultstream.monitoring;

import com.vaultstream.model.LedgerEntry;
import com.vaultstream.repository.AccountRepository;
import com.vaultstream.repository.LedgerEntryRepository;
import com.vaultstream.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LedgerHealthIndicatorTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @InjectMocks
    private LedgerHealthIndicator ledgerHealthIndicator;

    @Test
    void health_ShouldReturnUpStatus_WhenRepositoriesAreAccessibleAndLedgerBalanced() {
        when(accountRepository.count()).thenReturn(10L);
        when(transactionRepository.count()).thenReturn(25L);
        when(ledgerEntryRepository.sumAmountByDirection(LedgerEntry.Direction.DEBIT)).thenReturn(new BigDecimal("5000.00"));
        when(ledgerEntryRepository.sumAmountByDirection(LedgerEntry.Direction.CREDIT)).thenReturn(new BigDecimal("5000.00"));

        Health health = ledgerHealthIndicator.health();

        assertEquals(Status.UP, health.getStatus());
        assertEquals("operational", health.getDetails().get("ledger_status"));
        assertEquals(true, health.getDetails().get("ledger_balanced"));
        assertEquals(new BigDecimal("5000.00"), health.getDetails().get("total_debits"));
        assertEquals(new BigDecimal("5000.00"), health.getDetails().get("total_credits"));
        assertEquals(10L, health.getDetails().get("accounts"));
        assertEquals(25L, health.getDetails().get("transactions"));
        verify(accountRepository).count();
        verify(transactionRepository).count();
        verify(ledgerEntryRepository).sumAmountByDirection(LedgerEntry.Direction.DEBIT);
        verify(ledgerEntryRepository).sumAmountByDirection(LedgerEntry.Direction.CREDIT);
    }

    @Test
    void health_ShouldReturnDownStatus_WhenLedgerIsImbalanced() {
        when(accountRepository.count()).thenReturn(10L);
        when(transactionRepository.count()).thenReturn(25L);
        when(ledgerEntryRepository.sumAmountByDirection(LedgerEntry.Direction.DEBIT)).thenReturn(new BigDecimal("5000.00"));
        when(ledgerEntryRepository.sumAmountByDirection(LedgerEntry.Direction.CREDIT)).thenReturn(new BigDecimal("4900.00"));

        Health health = ledgerHealthIndicator.health();

        assertEquals(Status.DOWN, health.getStatus());
        assertEquals("imbalance_detected", health.getDetails().get("ledger_status"));
        assertEquals(false, health.getDetails().get("ledger_balanced"));
        assertEquals(new BigDecimal("5000.00"), health.getDetails().get("total_debits"));
        assertEquals(new BigDecimal("4900.00"), health.getDetails().get("total_credits"));
    }

    @Test
    void health_ShouldReturnDownStatus_WhenAccountRepositoryThrowsException() {
        when(accountRepository.count()).thenThrow(new RuntimeException("Database connection failed"));

        Health health = ledgerHealthIndicator.health();

        assertEquals(Status.DOWN, health.getStatus());
        assertTrue(health.getDetails().get("error").toString().contains("Database connection failed"));
        verify(accountRepository).count();
        verifyNoInteractions(transactionRepository);
        verifyNoInteractions(ledgerEntryRepository);
    }

    @Test
    void health_ShouldReturnDownStatus_WhenTransactionRepositoryThrowsException() {
        when(accountRepository.count()).thenReturn(10L);
        when(transactionRepository.count()).thenThrow(new RuntimeException("Query timeout"));

        Health health = ledgerHealthIndicator.health();

        assertEquals(Status.DOWN, health.getStatus());
        assertTrue(health.getDetails().get("error").toString().contains("Query timeout"));
        verify(accountRepository).count();
        verify(transactionRepository).count();
    }
}
