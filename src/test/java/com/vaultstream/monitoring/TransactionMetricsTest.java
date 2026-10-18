package com.vaultstream.monitoring;

import com.vaultstream.repository.TransactionRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionMetricsTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Counter depositCounter;

    @Mock
    private Counter withdrawalCounter;

    @Mock
    private Counter transferCounter;

    @InjectMocks
    private TransactionMetrics transactionMetrics;

    @BeforeEach
    void setUp() {
        // Mock the MeterRegistry.builder() calls
        when(meterRegistry.counter("vaultstream.transactions.deposit")).thenReturn(depositCounter);
        when(meterRegistry.counter("vaultstream.transactions.withdrawal")).thenReturn(withdrawalCounter);
        when(meterRegistry.counter("vaultstream.transactions.transfer")).thenReturn(transferCounter);
    }

    @Test
    void init_ShouldCreateAndRegisterCounters() {
        // Act
        transactionMetrics.init();

        // Assert
        verify(meterRegistry).counter("vaultstream.transactions.deposit");
        verify(meterRegistry).counter("vaultstream.transactions.withdrawal");
        verify(meterRegistry).counter("vaultstream.transactions.transfer");

        verify(depositCounter).register(meterRegistry);
        verify(withdrawalCounter).register(meterRegistry);
        verify(transferCounter).register(meterRegistry);
    }

    @Test
    void incrementDepositCount_ShouldIncrementCounter() {
        // Arrange
        transactionMetrics.init(); // Initialize counters

        // Act
        transactionMetrics.incrementDepositCount();

        // Assert
        verify(depositCounter).increment();
    }

    @Test
    void incrementWithdrawalCount_ShouldIncrementCounter() {
        // Arrange
        transactionMetrics.init(); // Initialize counters

        // Act
        transactionMetrics.incrementWithdrawalCount();

        // Assert
        verify(withdrawalCounter).increment();
    }

    @Test
    void incrementTransferCount_ShouldIncrementCounter() {
        // Arrange
        transactionMetrics.init(); // Initialize counters

        // Act
        transactionMetrics.incrementTransferCount();

        // Assert
        verify(transferCounter).increment();
    }

    @Test
    void getTotalTransactionCount_ShouldReturnRepositoryCount() {
        // Arrange
        long expectedCount = 128L;
        when(transactionRepository.count()).thenReturn(expectedCount);

        // Act
        long actualCount = transactionMetrics.getTotalTransactionCount();

        // Assert
        assertEquals(expectedCount, actualCount);
        verify(transactionRepository).count();
    }
}