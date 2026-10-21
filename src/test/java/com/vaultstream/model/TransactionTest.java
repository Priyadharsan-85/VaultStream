package com.vaultstream.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {
    @Test
    void builderStoresTransactionFields() {
        UUID id = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        LocalDateTime timestamp = LocalDateTime.now().minusMinutes(1);
        Transaction transaction = Transaction.builder().id(id).accountId(accountId)
                .amount(new BigDecimal("12.34")).type(Transaction.TransactionType.TRANSFER_OUT)
                .referenceId("ref-123").description("Transfer").timestamp(timestamp).build();

        assertEquals(id, transaction.getId());
        assertEquals(accountId, transaction.getAccountId());
        assertEquals(new BigDecimal("12.34"), transaction.getAmount());
        assertEquals(Transaction.TransactionType.TRANSFER_OUT, transaction.getType());
        assertEquals("ref-123", transaction.getReferenceId());
        assertEquals("Transfer", transaction.getDescription());
        assertEquals(timestamp, transaction.getTimestamp());
    }

    @Test
    void prePersistSetsCreatedTimeAndMissingTransactionTimestamp() {
        Transaction transaction = new Transaction();
        LocalDateTime before = LocalDateTime.now();

        transaction.onCreate();

        LocalDateTime after = LocalDateTime.now();
        assertNotNull(transaction.getCreatedAt());
        assertNotNull(transaction.getTimestamp());
        assertFalse(transaction.getCreatedAt().isBefore(before));
        assertFalse(transaction.getTimestamp().isAfter(after));
    }

    @Test
    void prePersistPreservesProvidedTransactionTimestamp() {
        LocalDateTime timestamp = LocalDateTime.now().minusDays(1);
        Transaction transaction = new Transaction();
        transaction.setTimestamp(timestamp);

        transaction.onCreate();

        assertEquals(timestamp, transaction.getTimestamp());
        assertNotNull(transaction.getCreatedAt());
    }

    @Test
    void exposesAllPersistedTransactionTypes() {
        assertEquals(6, Transaction.TransactionType.values().length);
        assertNotNull(Transaction.TransactionType.valueOf("TRANSFER_IN"));
    }
}
