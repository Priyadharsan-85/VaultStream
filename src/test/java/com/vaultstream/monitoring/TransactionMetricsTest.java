package com.vaultstream.monitoring;

import com.vaultstream.repository.TransactionRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionMetricsTest {

    @Mock
    private TransactionRepository transactionRepository;

    private SimpleMeterRegistry meterRegistry;
    private TransactionMetrics transactionMetrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        transactionMetrics = new TransactionMetrics(transactionRepository, meterRegistry);
        transactionMetrics.init();
    }

    @Test
    void initializesTransactionCounters() {
        assertEquals(0.0, meterRegistry.get("vaultstream.transactions.deposit").counter().count());
        assertEquals(0.0, meterRegistry.get("vaultstream.transactions.withdrawal").counter().count());
        assertEquals(0.0, meterRegistry.get("vaultstream.transactions.transfer").counter().count());
    }

    @Test
    void incrementsTransactionCounters() {
        transactionMetrics.incrementDepositCount();
        transactionMetrics.incrementWithdrawalCount();
        transactionMetrics.incrementTransferCount();

        assertEquals(1.0, meterRegistry.get("vaultstream.transactions.deposit").counter().count());
        assertEquals(1.0, meterRegistry.get("vaultstream.transactions.withdrawal").counter().count());
        assertEquals(1.0, meterRegistry.get("vaultstream.transactions.transfer").counter().count());
    }

    @Test
    void getTotalTransactionCountReturnsRepositoryCount() {
        when(transactionRepository.count()).thenReturn(42L);

        assertEquals(42L, transactionMetrics.getTotalTransactionCount());
        verify(transactionRepository).count();
    }
}
