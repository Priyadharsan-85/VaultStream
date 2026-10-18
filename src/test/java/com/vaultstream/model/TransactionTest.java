package com.vaultstream.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void transaction_ShouldBeValid_WhenAllFieldsAreValid() {
        // Arrange
        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .accountId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(1000.00))
                .type(Transaction.TransactionType.DEPOSIT)
                .referenceId("REF123456")
                .description("Test transaction")
                .timestamp(LocalDateTime.now())
                .build();

        // Act
        Set<ConstraintViolation<Transaction>> violations = validator.validate(transaction);

        // Assert
        assertTrue(violations.isEmpty(), "There should be no validation violations");
    }

    @Test
    void transaction_ShouldHaveInvalidAmount_WhenNull() {
        // Arrange
        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .accountId(UUID.randomUUID())
                .amount(null) // Null amount
                .type(Transaction.TransactionType.DEPOSIT)
                .referenceId("REF123456")
                .description("Test transaction")
                .timestamp(LocalDateTime.now())
                .build();

        // Act
        Set<ConstraintViolation<Transaction>> violations = validator.validate(transaction);

        // Assert
        assertFalse(violations.isEmpty(), "There should be validation violations");
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("notnull"));
    }

    @Test
    void transaction_ShouldHaveInvalidAmount_WhenZeroOrNegative() {
        // Arrange
        Transaction transactionZero = Transaction.builder()
                .id(UUID.randomUUID())
                .accountId(UUID.randomUUID())
                .amount(BigDecimal.ZERO) // Zero amount
                .type(Transaction.TransactionType.DEPOSIT)
                .referenceId("REF123456")
                .description("Test transaction")
                .timestamp(LocalDateTime.now())
                .build();

        Transaction transactionNegative = Transaction.builder()
                .id(UUID.randomUUID())
                .accountId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(-10.00)) // Negative amount
                .type(Transaction.TransactionType.DEPOSIT)
                .referenceId("REF123456")
                .description("Test transaction")
                .timestamp(LocalDateTime.now())
                .build();

        // Act
        Set<ConstraintViolation<Transaction>> violationsZero = validator.validate(transactionZero);
        Set<ConstraintViolation<Transaction>> violationsNegative = validator.validate(transactionNegative);

        // Assert
        assertFalse(violationsZero.isEmpty(), "There should be validation violations for zero amount");
        assertEquals(1, violationsZero.size());
        assertTrue(violationsZero.iterator().next().getMessage().contains("Positive"));

        assertFalse(violationsNegative.isEmpty(), "There should be validation violations for negative amount");
        assertEquals(1, violationsNegative.size());
        assertTrue(violationsNegative.iterator().next().getMessage().contains("Positive"));
    }

    @Test
    void transaction_ShouldHaveInvalidType_WhenNull() {
        // Arrange
        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .accountId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(1000.00))
                .type(null) // Null type
                .referenceId("REF123456")
                .description("Test transaction")
                .timestamp(LocalDateTime.now())
                .build();

        // Act
        Set<ConstraintViolation<Transaction>> violations = validator.validate(transaction);

        // Assert
        assertFalse(violations.isEmpty(), "There should be validation violations");
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("notnull"));
    }

    @Test
    void transaction_ShouldHaveValidReferenceIdLength() {
        // Arrange
        String validReferenceId = "A".repeat(100); // Exactly 100 characters (max allowed)
        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .accountId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(1000.00))
                .type(Transaction.TransactionType.DEPOSIT)
                .referenceId(validReferenceId)
                .description("Test transaction")
                .timestamp(LocalDateTime.now())
                .build();

        // Act
        Set<ConstraintViolation<Transaction>> violations = validator.validate(transaction);

        // Assert
        assertTrue(violations.isEmpty(), "There should be no validation violations for valid reference ID length");
    }

    @Test
    void transaction_ShouldHaveInvalidReferenceId_WhenTooLong() {
        // Arrange
        String tooLongReferenceId = "A".repeat(101); // 101 characters (too long)
        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .accountId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(1000.00))
                .type(Transaction.TransactionType.DEPOSIT)
                .referenceId(tooLongReferenceId)
                .description("Test transaction")
                .timestamp(LocalDateTime.now())
                .build();

        // Act
        Set<ConstraintViolation<Transaction>> violations = validator.validate(transaction);

        // Assert
        assertFalse(violations.isEmpty(), "There should be validation violations for too long reference ID");
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("size"));
    }

    @Test
    void transaction_ShouldHaveValidDescriptionLength() {
        // Arrange
        String validDescription = "A".repeat(255); // Exactly 255 characters (max allowed)
        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .accountId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(1000.00))
                .type(Transaction.TransactionType.DEPOSIT)
                .referenceId("REF123")
                .description(validDescription)
                .timestamp(LocalDateTime.now())
                .build();

        // Act
        Set<ConstraintViolation<Transaction>> violations = validator.validate(transaction);

        // Assert
        assertTrue(violations.isEmpty(), "There should be no validation violations for valid description length");
    }

    @Test
    void transaction_ShouldHaveInvalidDescription_WhenTooLong() {
        // Arrange
        String tooLongDescription = "A".repeat(256); // 256 characters (too long)
        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .accountId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(1000.00))
                .type(Transaction.TransactionType.DEPOSIT)
                .referenceId("REF123")
                .description(tooLongDescription)
                .timestamp(LocalDateTime.now())
                .build();

        // Act
        Set<ConstraintViolation<Transaction>> violations = validator.validate(transaction);

        // Assert
        assertFalse(violations.isEmpty(), "There should be validation violations for too long description");
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("size"));
    }

    @Test
    void transaction_ShouldHaveValidEnumValues() {
        // Arrange & Act & Assert
        for (Transaction.TransactionType type : Transaction.TransactionType.values()) {
            Transaction transaction = Transaction.builder()
                    .id(UUID.randomUUID())
                    .accountId(UUID.randomUUID())
                    .amount(BigDecimal.valueOf(1000.00))
                    .type(type)
                    .referenceId("REF123")
                    .description("Test transaction")
                    .timestamp(LocalDateTime.now())
                    .build();

            Set<ConstraintViolation<Transaction>> violations = validator.validate(transaction);
            assertTrue(violations.isEmpty(), "TransactionType " + type + " should be valid");
        }
    }

    @Test
    void transaction_ShouldHaveInvalidTimestamp_WhenNull() {
        // Arrange
        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .accountId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(1000.00))
                .type(Transaction.TransactionType.DEPOSIT)
                .referenceId("REF123")
                .description("Test transaction")
                .timestamp(null) // Null timestamp
                .build();

        // Act
        Set<ConstraintViolation<Transaction>> violations = validator.validate(transaction);

        // Assert
        assertFalse(violations.isEmpty(), "There should be validation violations");
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("notnull"));
    }

    @Test
    void transaction_Builder_ShouldCreateTransactionWithAllFields() {
        // Arrange
        UUID testId = UUID.randomUUID();
        UUID testAccountId = UUID.randomUUID();
        BigDecimal testAmount = BigDecimal.valueOf(2500.75);
        Transaction.TransactionType testType = Transaction.TransactionType.TRANSFER_OUT;
        String testReferenceId = "TXF789012";
        String testDescription = "Transfer to savings account";
        LocalDateTime testTimestamp = LocalDateTime.now();
        LocalDateTime testCreatedAt = LocalDateTime.now().minusHours(1);

        // Act
        Transaction transaction = Transaction.builder()
                .id(testId)
                .accountId(testAccountId)
                .amount(testAmount)
                .type(testType)
                .referenceId(testReferenceId)
                .description(testDescription)
                .timestamp(testTimestamp)
                .createdAt(testCreatedAt)
                .build();

        // Assert
        assertEquals(testId, transaction.getId());
        assertEquals(testAccountId, transaction.getAccountId());
        assertEquals(testAmount, transaction.getAmount());
        assertEquals(testType, transaction.getType());
        assertEquals(testReferenceId, transaction.getReferenceId());
        assertEquals(testDescription, transaction.getDescription());
        assertEquals(testTimestamp, transaction.getTimestamp());
        assertEquals(testCreatedAt, transaction.getCreatedAt());
    }

    @Test
    void transaction_ShouldSetTimestamps_OnCreate() {
        // Arrange
        Transaction transaction = new Transaction();
        transaction.setAccountId(UUID.randomUUID());
        transaction.setAmount(BigDecimal.valueOf(1000.00));
        transaction.setType(Transaction.TransactionType.DEPOSIT);
        transaction.setReferenceId("REF123");
        transaction.setDescription("Test transaction");

        // Act
        LocalDateTime beforeCreate = LocalDateTime.now();
        // Simulate @PrePersist by calling onCreate directly (normally called by Hibernate)
        transaction.onCreate();
        LocalDateTime afterCreate = LocalDateTime.now();

        // Assert
        assertNotNull(transaction.getCreatedAt());
        assertNotNull(transaction.getTimestamp()); // timestamp should be set if null
        assertFalse(transaction.getCreatedAt().isBefore(beforeCreate));
        assertFalse(transaction.getCreatedAt().isAfter(afterCreate));
        // timestamp should be set to now if it was null
        assertFalse(transaction.getTimestamp().isBefore(beforeCreate));
        assertFalse(transaction.getTimestamp().isAfter(afterCreate));
    }
}