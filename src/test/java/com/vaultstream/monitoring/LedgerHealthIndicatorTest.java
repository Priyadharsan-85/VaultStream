package com.vaultstream.monitoring;

import com.vaultstream.repository.AccountRepository;
import com.vaultstream.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LedgerHealthIndicatorTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private LedgerHealthIndicator ledgerHealthIndicator;

    @BeforeEach
    void setUp() {
        // Mock repository behavior
        when(accountRepository.count()).thenReturn(10L);
        when(transactionRepository.count()).thenReturn(25L);
    }

    @Test
    void health_ShouldReturnUpStatus_WhenRepositoriesAreAccessible() {
        // Act
        Health health = ledgerHealthIndicator.health();

        // Assert
        assertEquals(Status.UP, health.getStatus());
        assertEquals("operational", health.getDetail("ledger_status"));
        assertEquals(10L, health.getDetail("accounts"));
        assertEquals(25L, health.getDetail("transactions"));
        verify(accountRepository).count();
        verify(transactionRepository).count();
    }

    @Test
    void health_ShouldReturnDownStatus_WhenAccountRepositoryThrowsException() {
        // Arrange
        when(accountRepository.count()).thenThrow(new RuntimeException("Database connection failed"));
        when(transactionRepository.count()).thenReturn(25L);

        // Act
        Health health = ledgerHealthIndicator.health();

        // Assert
        assertEquals(Status.DOWN, health.getStatus());
        assertTrue(health.getDetail("error").toString().contains("Database connection failed"));
        verify(accountRepository).count();
        verify(transactionRepository).count();
    }

    @Test
    void health_ShouldReturnDownStatus_WhenTransactionRepositoryThrowsException() {
        // Arrange
        when(accountRepository.count()).thenReturn(10L);
        when(transactionRepository.count()).thenThrow(new RuntimeException("Query timeout"));

        // Act
        Health health = ledgerHealthIndicator.health();

        // Assert
        assertEquals(Status.DOWN, health.getStatus());
        assertTrue(health.getDetail("error").toString().contains("Query timeout"));
        verify(accountRepository).count();
        verify(transactionRepository).count();
    }

    @Test
    void health_ShouldReturnDownStatus_WhenBothRepositoriesThrowException() {
        // Arrange
        when(accountRepository.count()).thenThrow(new RuntimeException("Account DB error"));
        when(transactionRepository.count()).thenThrow(new RuntimeException("Transaction DB error"));

        // Act
        Health health = ledgerHealthIndicator.health();

        // Assert
        assertEquals(Status.DOWN, health.getStatus());
        // Should contain one of the error messages
        assertTrue(health.getDetail("error").toString().contains("Account DB error") ||
                   health.getDetail("error").toString().contains("Transaction DB error"));
        verify(accountRepository).count();
        verify(transactionRepository).count();
    }
}