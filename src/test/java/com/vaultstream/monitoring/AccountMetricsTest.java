package com.vaultstream.monitoring;

import com.vaultstream.repository.AccountRepository;
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
class AccountMetricsTest {

    @Mock
    private AccountRepository accountRepository;

    private SimpleMeterRegistry meterRegistry;
    private AccountMetrics accountMetrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        accountMetrics = new AccountMetrics(accountRepository, meterRegistry);
        accountMetrics.init();
    }

    @Test
    void initializesAccountCounters() {
        assertEquals(0.0, meterRegistry.get("vaultstream.accounts.created").counter().count());
        assertEquals(0.0, meterRegistry.get("vaultstream.accounts.updated").counter().count());
        assertEquals(0.0, meterRegistry.get("vaultstream.accounts.deleted").counter().count());
    }

    @Test
    void incrementsAccountCounters() {
        accountMetrics.incrementAccountCreatedCount();
        accountMetrics.incrementAccountUpdatedCount();
        accountMetrics.incrementAccountDeletedCount();

        assertEquals(1.0, meterRegistry.get("vaultstream.accounts.created").counter().count());
        assertEquals(1.0, meterRegistry.get("vaultstream.accounts.updated").counter().count());
        assertEquals(1.0, meterRegistry.get("vaultstream.accounts.deleted").counter().count());
    }

    @Test
    void getTotalAccountCountReturnsRepositoryCount() {
        when(accountRepository.count()).thenReturn(42L);

        assertEquals(42L, accountMetrics.getTotalAccountCount());
        verify(accountRepository).count();
    }
}
