package com.vaultstream.demo;

import com.vaultstream.dto.TransactionDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** A generated event for the public portfolio's simulated live ledger feed. */
public record DemoLiveEventDto(
        UUID id,
        String label,
        BigDecimal amount,
        LocalDateTime timestamp,
        List<TransactionDto> transactions) {
}
