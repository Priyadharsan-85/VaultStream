package com.vaultstream.monitoring;

import com.vaultstream.repository.AccountRepository;
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
class AccountMetricsTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Counter accountCreatedCounter;

    @Mock
    private Counter accountUpdatedCounter;

    @Mock
    private Counter accountDeletedCounter;

    @InjectMocks
    private AccountMetrics accountMetrics;

    @BeforeEach
    void setUp() {
        // Mock the MeterRegistry.builder() calls
        when(meterRegistry.counter("vaultstream.accounts.created")).thenReturn(accountCreatedCounter);
        when(meterRegistry.counter("vaultstream.accounts.updated")).thenReturn(accountUpdatedCounter);
        when(meterRegistry.counter("vaultstream.accounts.deleted")).thenReturn(accountDeletedCounter);
    }

    @Test
    void init_ShouldCreateAndRegisterCounters() {
        // Act
        accountMetrics.init();

        // Assert
        verify(meterRegistry).counter("vaultstream.accounts.created");
        verify(meterRegistry).counter("vaultstream.accounts.updated");
        verify(meterRegistry).counter("vaultstream.accounts.deleted");

        verify(accountCreatedCounter).register(meterRegistry);
        verify(accountUpdatedCounter).register(meterRegistry);
        verify(accountDeletedCounter).register(meterRegistry);
    }

    @Test
    void incrementAccountCreatedCount_ShouldIncrementCounter() {
        // Arrange
        accountMetrics.init(); // Initialize counters

        // Act
        accountMetrics.incrementAccountCreatedCount();

        // Assert
        verify(accountCreatedCounter).increment();
    }

    @Test
    void incrementAccountUpdatedCount_ShouldIncrementCounter() {
        // Arrange
        accountMetrics.init(); // Initialize counters

        // Act
        accountMetrics.incrementAccountUpdatedCount();

        // Assert
        verify(accountUpdatedCounter).increment();
    }

    @Test
    void incrementAccountDeletedCount_ShouldIncrementCounter() {
        // Arrange
        accountMetrics.init(); // Initialize counters

        // Act
        accountMetrics.incrementAccountDeletedCount();

        // Assert
        verify(accountDeletedCounter).increment();
    }

    @Test
    void getTotalAccountCount_ShouldReturnRepositoryCount() {
        // Arrange
        long expectedCount = 42L;
        when(accountRepository.count()).thenReturn(expectedCount);

        // Act
        long actualCount = accountMetrics.getTotalAccountCount();

        // Assert
        assertEquals(expectedCount, actualCount);
        verify(accountRepository).count();
    }
}