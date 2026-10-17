package com.vaultstream.monitoring;

import com.vaultstream.repository.TransactionRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
@RequiredArgsConstructor
public class TransactionMetrics {

    private final TransactionRepository transactionRepository;
    private final MeterRegistry meterRegistry;

    private Counter depositCounter;
    private Counter withdrawalCounter;
    private Counter transferCounter;

    @PostConstruct
    public void init() {
        depositCounter = Counter.builder("vaultstream.transactions.deposit")
                .description("Number of deposit transactions")
                .register(meterRegistry);

        withdrawalCounter = Counter.builder("vaultstream.transactions.withdrawal")
                .description("Number of withdrawal transactions")
                .register(meterRegistry);

        transferCounter = Counter.builder("vaultstream.transactions.transfer")
                .description("Number of transfer transactions")
                .register(meterRegistry);
    }

    public void incrementDepositCount() {
        depositCounter.increment();
    }

    public void incrementWithdrawalCount() {
        withdrawalCounter.increment();
    }

    public void incrementTransferCount() {
        transferCounter.increment();
    }

    public long getTotalTransactionCount() {
        return transactionRepository.count();
    }
}