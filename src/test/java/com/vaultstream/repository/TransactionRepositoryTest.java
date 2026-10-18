package com.vaultstream.repository;

import com.vaultstream.model.Transaction;
import com.vaultstream.model.Transaction.TransactionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(org.springframework.test.context.junit.jupiter.SpringExtension.class)
@DataJpaTest
@Import(TransactionRepository.class)
class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    void repository_ShouldNotBeNull() {
        assertNotNull(transactionRepository);
    }

    @Test
    void findByReferenceId_ShouldReturnEmptyOptional_WhenNoTransactionExists() {
        // Act
        Optional<Transaction> result = transactionRepository.findByReferenceId("NONEXISTENT");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void findByAccountId_ShouldReturnEmptyList_WhenNoTransactionsExist() {
        // Act
        List<Transaction> result = transactionRepository.findByAccountId(UUID.randomUUID());

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void findByAccountIdOrderByTimestampDesc_ShouldReturnEmptyList_WhenNoTransactionsExist() {
        // Act
        List<Transaction> result = transactionRepository.findByAccountIdOrderByTimestampDesc(UUID.randomUUID());

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void findByAccountIdAndTimeRange_ShouldReturnEmptyList_WhenNoTransactionsExist() {
        // Act
        List<Transaction> result = transactionRepository.findByAccountIdAndTimeRange(
                UUID.randomUUID(),
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now()
        );

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void saveAndFindById_ShouldWorkCorrectly() {
        // Arrange
        Transaction transaction = Transaction.builder()
                .accountId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(100.00))
                .type(TransactionType.DEPOSIT)
                .referenceId("REF123")
                .description("Test transaction")
                .build();

        // Act
        Transaction savedTransaction = transactionRepository.save(transaction);
        Optional<Transaction> foundTransaction = transactionRepository.findById(savedTransaction.getId());

        // Assert
        assertNotNull(savedTransaction.getId());
        assertTrue(foundTransaction.isPresent());
        assertEquals(savedTransaction.getAccountId(), foundTransaction.get().getAccountId());
        assertEquals(BigDecimal.valueOf(100.00), foundTransaction.get().getAmount());
        assertEquals(TransactionType.DEPOSIT, foundTransaction.get().getType());
        assertEquals("REF123", foundTransaction.get().getReferenceId());
        assertEquals("Test transaction", foundTransaction.get().getDescription());
    }

    @Test
    void findByReferenceId_ShouldReturnTransaction_WhenTransactionExists() {
        // Arrange
        Transaction transaction = Transaction.builder()
                .accountId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(200.00))
                .type(TransactionType.WITHDRAWAL)
                .referenceId("FOUND456")
                .description("Found transaction")
                .build();
        transactionRepository.save(transaction);

        // Act
        Optional<Transaction> result = transactionRepository.findByReferenceId("FOUND456");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("FOUND456", result.get().getReferenceId());
        assertEquals(BigDecimal.valueOf(200.00), result.get().getAmount());
        assertEquals(TransactionType.WITHDRAWAL, result.get().getType());
    }

    @Test
    void findByAccountId_ShouldReturnTransactions_WhenTransactionsExist() {
        // Arrange
        UUID accountId = UUID.randomUUID();
        Transaction transaction1 = Transaction.builder()
                .accountId(accountId)
                .amount(BigDecimal.valueOf(100.00))
                .type(TransactionType.DEPOSIT)
                .referenceId("REF1")
                .description("First transaction")
                .build();
        Transaction transaction2 = Transaction.builder()
                .accountId(accountId)
                .amount(BigDecimal.valueOf(50.00))
                .type(TransactionType.WITHDRAWAL)
                .referenceId("REF2")
                .description("Second transaction")
                .build();
        transactionRepository.save(transaction1);
        transactionRepository.save(transaction2);

        // Act
        List<Transaction> result = transactionRepository.findByAccountId(accountId);

        // Assert
        assertFalse(result.isEmpty());
        assertEquals(2, result.size());
        // Note: Without explicit ordering, we can't guarantee the order
        boolean hasRef1 = result.stream().anyMatch(t -> "REF1".equals(t.getReferenceId()));
        boolean hasRef2 = result.stream().anyMatch(t -> "REF2".equals(t.getReferenceId()));
        assertTrue(hasRef1 && hasRef2);
    }

    @Test
    void findByAccountIdOrderByTimestampDesc_ShouldReturnTransactionsInDescendingOrder() {
        // Arrange
        UUID accountId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Transaction olderTransaction = Transaction.builder()
                .accountId(accountId)
                .amount(BigDecimal.valueOf(100.00))
                .type(TransactionType.DEPOSIT)
                .referenceId("OLDER")
                .description("Older transaction")
                .timestamp(now.minusHours(2))
                .build();
        Transaction newerTransaction = Transaction.builder()
                .accountId(accountId)
                .amount(BigDecimal.valueOf(50.00))
                .type(TransactionType.WITHDRAWAL)
                .referenceId("NEWER")
                .description("Newer transaction")
                .timestamp(now.minusHours(1))
                .build();
        transactionRepository.save(olderTransaction);
        transactionRepository.save(newerTransaction);

        // Act
        List<Transaction> result = transactionRepository.findByAccountIdOrderByTimestampDesc(accountId);

        // Assert
        assertFalse(result.isEmpty());
        assertEquals(2, result.size());
        assertEquals("NEWER", result.get(0).getReferenceId()); // Most recent first
        assertEquals("OLDER", result.get(1).getReferenceId()); // Older second
    }

    @Test
    void findByAccountIdAndTimeRange_ShouldReturnTransactionsInRange() {
        // Arrange
        UUID accountId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Transaction oldTransaction = Transaction.builder()
                .accountId(accountId)
                .amount(BigDecimal.valueOf(100.00))
                .type(TransactionType.DEPOSIT)
                .referenceId("OLD")
                .description("Old transaction")
                .timestamp(now.minusDays(2))
                .build();
        Transaction recentTransaction = Transaction.builder()
                .accountId(accountId)
                .amount(BigDecimal.valueOf(50.00))
                .type(TransactionType.WITHDRAWAL)
                .referenceId("RECENT")
                .description("Recent transaction")
                .timestamp(now.minusHours(1))
                .build();
        Transaction futureTransaction = Transaction.builder()
                .accountId(accountId)
                .amount(BigDecimal.valueOf(75.00))
                .type(TransactionType.DEPOSIT)
                .referenceId("FUTURE")
                .description("Future transaction")
                .timestamp(now.plusDays(1))
                .build();
        transactionRepository.save(oldTransaction);
        transactionRepository.save(recentTransaction);
        transactionRepository.save(futureTransaction);

        // Act
        List<Transaction> result = transactionRepository.findByAccountIdAndTimeRange(
                accountId,
                now.minusDays(1),
                now.plusHours(1)
        );

        // Assert
        assertFalse(result.isEmpty());
        assertEquals(1, result.size()); // Only the recent transaction should be in range
        assertEquals("RECENT", result.get(0).getReferenceId());
    }

    @Test
    void sumAmountByAccountIdAndType_ShouldReturnCorrectSum() {
        // Arrange
        UUID accountId = UUID.randomUUID();
        Transaction deposit1 = Transaction.builder()
                .accountId(accountId)
                .amount(BigDecimal.valueOf(100.00))
                .type(TransactionType.DEPOSIT)
                .referenceId("DEP1")
                .description("First deposit")
                .build();
        Transaction deposit2 = Transaction.builder()
                .accountId(accountId)
                .amount(BigDecimal.valueOf(50.00))
                .type(TransactionType.DEPOSIT)
                .referenceId("DEP2")
                .description("Second deposit")
                .build();
        Transaction withdrawal = Transaction.builder()
                .accountId(accountId)
                .amount(BigDecimal.valueOf(30.00))
                .type(TransactionType.WITHDRAWAL)
                .referenceId("WDL1")
                .description("Withdrawal")
                .build();
        transactionRepository.save(deposit1);
        transactionRepository.save(deposit2);
        transactionRepository.save(withdrawal);

        // Act
        BigDecimal depositSum = transactionRepository.sumAmountByAccountIdAndType(accountId, "DEPOSIT");
        BigDecimal withdrawalSum = transactionRepository.sumAmountByAccountIdAndType(accountId, "WITHDRAWAL");

        // Assert
        assertEquals(BigDecimal.valueOf(150.00), depositSum);
        assertEquals(BigDecimal.valueOf(30.00), withdrawalSum);
    }
}