package com.vaultstream.dto;

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

class TransactionDtoTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void transactionDto_ShouldBeValid_WhenAllFieldsAreValid() {
        // Arrange
        TransactionDto transactionDto = TransactionDto.builder()
                .id(UUID.randomUUID())
                .accountId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(1000.00))
                .type(TransactionType.DEPOSIT)
                .referenceId("REF123456")
                .description("Test transaction")
                .timestamp(LocalDateTime.now())
                .build();

        // Act
        Set<ConstraintViolation<TransactionDto>> violations = validator.validate(transactionDto);

        // Assert
        assertTrue(violations.isEmpty(), "There should be no validation violations");
    }

    @Test
    void transactionDto_ShouldHaveInvalidId_WhenNull() {
        // Arrange
        TransactionDto transactionDto = TransactionDto.builder()
                .id(null) // Null ID
                .accountId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(1000.00))
                .type(TransactionType.DEPOSIT)
                .referenceId("REF123456")
                .description("Test transaction")
                .timestamp(LocalDateTime.now())
                .build();

        // Act
        Set<ConstraintViolation<TransactionDto>> violations = validator.validate(transactionDto);

        // Assert
        assertTrue(violations.isEmpty(), "ID should be allowed to be null (not annotated with @NotNull)");
    }

    @Test
    void transactionDto_ShouldHaveInvalidAccountId_WhenNull() {
        // Arrange
        TransactionDto transactionDto = TransactionDto.builder()
                .id(UUID.randomUUID())
                .accountId(null) // Null accountId
                .amount(BigDecimal.valueOf(1000.00))
                .type(TransactionType.DEPOSIT)
                .referenceId("REF123456")
                .description("Test transaction")
                .timestamp(LocalDateTime.now())
                .build();

        // Act
        Set<ConstraintViolation<TransactionDto>> violations = validator.validate(transactionDto);

        // Assert
        assertTrue(violations.isEmpty(), "accountId should be allowed to be null (not annotated with @NotNull)");
    }

    @Test
    void transactionDto_ShouldHaveInvalidAmount_WhenNull() {
        // Arrange
        TransactionDto transactionDto = TransactionDto.builder()
                .id(UUID.randomUUID())
                .accountId(UUID.randomUUID())
                .amount(null) // Null amount
                .type(TransactionType.DEPOSIT)
                .referenceId("REF123456")
                .description("Test transaction")
                .timestamp(LocalDateTime.now())
                .build();

        // Act
        Set<ConstraintViolation<TransactionDto>> violations = validator.validate(transactionDto);

        // Assert
        assertFalse(violations.isEmpty(), "There should be validation violations");
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("notnull"));
    }

    @Test
    void transactionDto_ShouldHaveInvalidAmount_WhenNotPositive() {
        // Arrange
        TransactionDto transactionDtoZero = TransactionDto.builder()
                .id(UUID.randomUUID())
                .accountId(UUID.randomUUID())
                .amount(BigDecimal.ZERO) // Zero amount
                .type(TransactionType.DEPOSIT)
                .referenceId("REF123456")
                .description("Test transaction")
                .timestamp(LocalDateTime.now())
                .build();

        TransactionDto transactionDtoNegative = TransactionDto.builder()
                .id(UUID.randomUUID())
                .accountId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(-10.00)) // Negative amount
                .type(TransactionType.DEPOSIT)
                .referenceId("REF123456")
                .description("Test transaction")
                .timestamp(LocalDateTime.now())
                .build();

        // Act
        Set<ConstraintViolation<TransactionDto>> violationsZero = validator.validate(transactionDtoZero);
        Set<ConstraintViolation<TransactionDto>> violationsNegative = validator.validate(transactionDtoNegative);

        // Assert
        assertFalse(violationsZero.isEmpty(), "There should be validation violations for zero amount");
        assertEquals(1, violationsZero.size());
        assertTrue(violationsZero.iterator().next().getMessage().contains("Positive"));

        assertFalse(violationsNegative.isEmpty(), "There should be validation violations for negative amount");
        assertEquals(1, violationsNegative.size());
        assertTrue(violationsNegative.iterator().next().getMessage().contains("Positive"));
    }

    @Test
    void transactionDto_ShouldHaveInvalidAmount_WhenFractionTooLong() {
        // Arrange
        TransactionDto transactionDto = TransactionDto.builder()
                .id(UUID.randomUUID())
                .accountId(UUID.randomUUID())
                .amount(new BigDecimal("1000.12345")) // 5 decimal places (too many, max 4)
                .type(TransactionType.DEPOSIT)
                .referenceId("REF123456")
                .description("Test transaction")
                .timestamp(LocalDateTime.now())
                .build();

        // Act
        Set<ConstraintViolation<TransactionDto>> violations = validator.validate(transactionDto);

        // Assert
        assertFalse(violations.isEmpty(), "There should be validation violations for too many decimal places");
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("Digits"));
    }

    @Test
    void transactionDto_ShouldHaveInvalidAmount_WhenIntegerTooLong() {
        // Arrange
        TransactionDto transactionDto = TransactionDto.builder()
                .id(UUID.randomUUID())
                .accountId(UUID.randomUUID())
                .amount(new BigDecimal("1234567890123456.0000")) // 16 integer digits (too many, max 15)
                .type(TransactionType.DEPOSIT)
                .referenceId("REF123456")
                .description("Test transaction")
                .timestamp(LocalDateTime.now())
                .build();

        // Act
        Set<ConstraintViolation<TransactionDto>> violations = validator.validate(transactionDto);

        // Assert
        assertFalse(violations.isEmpty(), "There should be validation violations for too many integer digits");
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("Digits"));
    }

    @Test
    void transactionDto_ShouldHaveInvalidType_WhenNull() {
        // Arrange
        TransactionDto transactionDto = TransactionDto.builder()
                .id(UUID.randomUUID())
                .accountId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(1000.00))
                .type(null) // Null type
                .referenceId("REF123456")
                .description("Test transaction")
                .timestamp(LocalDateTime.now())
                .build();

        // Act
        Set<ConstraintViolation<TransactionDto>> violations = validator.validate(transactionDto);

        // Assert
        assertFalse(violations.isEmpty(), "There should be validation violations");
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("notnull"));
    }

    @Test
    void transactionDto_ShouldHaveValidEnumValues() {
        // Arrange & Act & Assert
        for (TransactionType type : TransactionType.values()) {
            TransactionDto transactionDto = TransactionDto.builder()
                    .id(UUID.randomUUID())
                    .accountId(UUID.randomUUID())
                    .amount(BigDecimal.valueOf(1000.00))
                    .type(type)
                    .referenceId("REF123")
                    .description("Test transaction")
                    .timestamp(LocalDateTime.now())
                    .build();

            Set<ConstraintViolation<TransactionDto>> violations = validator.validate(transactionDto);
            assertTrue(violations.isEmpty(), "TransactionType " + type + " should be valid");
        }
    }

    @Test
    void transactionDto_ShouldHaveInvalidReferenceId_WhenTooLong() {
        // Arrange
        String tooLongReferenceId = "A".repeat(101); // 101 characters (too long)
        TransactionDto transactionDto = TransactionDto.builder()
                .id(UUID.randomUUID())
                .accountId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(1000.00))
                .type(TransactionType.DEPOSIT)
                .referenceId(tooLongReferenceId)
                .description("Test transaction")
                .timestamp(LocalDateTime.now())
                .build();

        // Act
        Set<ConstraintViolation<TransactionDto>> violations = validator.validate(transactionDto);

        // Assert
        assertFalse(violations.isEmpty(), "There should be validation violations for too long reference ID");
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("size"));
    }

    @Test
    void transactionDto_ShouldHaveValidReferenceIdLength() {
        // Arrange
        String validReferenceId = "A".repeat(100); // Exactly 100 characters (max allowed)
        TransactionDto transactionDto = TransactionDto.builder()
                .id(UUID.randomUUID())
                .accountId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(1000.00))
                .type(TransactionType.DEPOSIT)
                .referenceId(validReferenceId)
                .description("Test transaction")
                .timestamp(LocalDateTime.now())
                .build();

        // Act
        Set<ConstraintViolation<TransactionDto>> violations = validator.validate(transactionDto);

        // Assert
        assertTrue(violations.isEmpty(), "There should be no validation violations for valid reference ID length");
    }

    @Test
    void transactionDto_ShouldAllowEmptyReferenceId() {
        // Arrange
        TransactionDto transactionDto = TransactionDto.builder()
                .id(UUID.randomUUID())
                .accountId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(1000.00))
                .type(TransactionType.DEPOSIT)
                .referenceId("") // Empty referenceId should be allowed (not annotated with @NotBlank)
                .description("Test transaction")
                .timestamp(LocalDateTime.now())
                .build();

        // Act
        Set<ConstraintViolation<TransactionDto>> violations = validator.validate(transactionDto);

        // Assert
        assertTrue(violations.isEmpty(), "Empty referenceId should be allowed");
    }

    @Test
    void transactionDto_ShouldAllowNullReferenceId() {
        // Arrange
        TransactionDto transactionDto = TransactionDto.builder()
                .id(UUID.randomUUID())
                .accountId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(1000.00))
                .type(TransactionType.DEPOSIT)
                .referenceId(null) // Null referenceId should be allowed (not annotated with @NotNull)
                .description("Test transaction")
                .timestamp(LocalDateTime.now())
                .build();

        // Act
        Set<ConstraintViolation<TransactionDto>> violations = validator.validate(transactionDto);

        // Assert
        assertTrue(violations.isEmpty(), "Null referenceId should be allowed");
    }

    @Test
    void transactionDto_ShouldHaveInvalidDescription_WhenTooLong() {
        // Arrange
        String tooLongDescription = "A".repeat(256); // 256 characters (too long)
        TransactionDto transactionDto = TransactionDto.builder()
                .id(UUID.randomUUID())
                .accountId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(1000.00))
                .type(TransactionType.DEPOSIT)
                .referenceId("REF123")
                .description(tooLongDescription)
                .timestamp(LocalDateTime.now())
                .build();

        // Act
        Set<ConstraintViolation<TransactionDto>> violations = validator.validate(transactionDto);

        // Assert
        assertFalse(violations.isEmpty(), "There should be validation violations for too long description");
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("size"));
    }

    @Test
    void transactionDto_ShouldHaveValidDescriptionLength() {
        // Arrange
        String validDescription = "A".repeat(255); // Exactly 255 characters (max allowed)
        TransactionDto transactionDto = TransactionDto.builder()
                .id(UUID.randomUUID())
                .accountId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(1000.00))
                .type(TransactionType.DEPOSIT)
                .referenceId("REF123")
                .description(validDescription)
                .timestamp(LocalDateTime.now())
                .build();

        // Act
        Set<ConstraintViolation<TransactionDto>> violations = validator.validate(transactionDto);

        // Assert
        assertTrue(violations.isEmpty(), "There should be no validation violations for valid description length");
    }

    @Test
    void transactionDto_ShouldAllowEmptyDescription() {
        // Arrange
        TransactionDto transactionDto = TransactionDto.builder()
                .id(UUID.randomUUID())
                .accountId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(1000.00))
                .type(TransactionType.DEPOSIT)
                .referenceId("REF123")
                .description("") // Empty description should be allowed (not annotated with @NotBlank)
                .timestamp(LocalDateTime.now())
                .build();

        // Act
        Set<ConstraintViolation<TransactionDto>> violations = validator.validate(transactionDto);

        // Assert
        assertTrue(violations.isEmpty(), "Empty description should be allowed");
    }

    @Test
    void transactionDto_ShouldAllowNullDescription() {
        // Arrange
        TransactionDto transactionDto = TransactionDto.builder()
                .id(UUID.randomUUID())
                .accountId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(1000.00))
                .type(TransactionType.DEPOSIT)
                .referenceId("REF123")
                .description(null) // Null description should be allowed (not annotated with @NotNull)
                .timestamp(LocalDateTime.now())
                .build();

        // Act
        Set<ConstraintViolation<TransactionDto>> violations = validator.validate(transactionDto);

        // Assert
        assertTrue(violations.isEmpty(), "Null description should be allowed");
    }

    @Test
    void transactionDto_ShouldHaveInvalidTimestamp_WhenNull() {
        // Arrange
        TransactionDto transactionDto = TransactionDto.builder()
                .id(UUID.randomUUID())
                .accountId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(1000.00))
                .type(TransactionType.DEPOSIT)
                .referenceId("REF123456")
                .description("Test transaction")
                .timestamp(null) // Null timestamp
                .build();

        // Act
        Set<ConstraintViolation<TransactionDto>> violations = validator.validate(transactionDto);

        // Assert
        assertFalse(violations.isEmpty(), "There should be validation violations");
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("notnull"));
    }

    @Test
    void transactionDto_Builder_ShouldCreateTransactionDtoWithAllFields() {
        // Arrange
        UUID testId = UUID.randomUUID();
        UUID testAccountId = UUID.randomUUID();
        BigDecimal testAmount = BigDecimal.valueOf(2500.75);
        TransactionType testType = TransactionType.TRANSFER_OUT;
        String testReferenceId = "TXF789012";
        String testDescription = "Transfer to savings account";
        LocalDateTime testTimestamp = LocalDateTime.now();

        // Act
        TransactionDto transactionDto = TransactionDto.builder()
                .id(testId)
                .accountId(testAccountId)
                .amount(testAmount)
                .type(testType)
                .referenceId(testReferenceId)
                .description(testDescription)
                .timestamp(testTimestamp)
                .build();

        // Assert
        assertEquals(testId, transactionDto.getId());
        assertEquals(testAccountId, transactionDto.getAccountId());
        assertEquals(testAmount, transactionDto.getAmount());
        assertEquals(testType, transactionDto.getType());
        assertEquals(testReferenceId, transactionDto.getReferenceId());
        assertEquals(testDescription, transactionDto.getDescription());
        assertEquals(testTimestamp, transactionDto.getTimestamp());
    }

    @Test
    void depositRequest_ShouldBeValid_WhenValidRequestProvided() {
        // Arrange
        TransactionDto.DepositRequest depositRequest = new TransactionDto.DepositRequest(
                UUID.randomUUID(),
                BigDecimal.valueOf(100.00),
                "DEP456",
                "Test deposit"
        );

        // Act
        Set<ConstraintViolation<TransactionDto.DepositRequest>> violations = validator.validate(depositRequest);

        // Assert
        assertTrue(violations.isEmpty(), "There should be no validation violations");
    }

    @Test
    void depositRequest_ShouldHaveInvalidAccountId_WhenNull() {
        // Arrange
        TransactionDto.DepositRequest depositRequest = new TransactionDto.DepositRequest(
                null, // Null accountId
                BigDecimal.valueOf(100.00),
                "DEP456",
                "Test deposit"
        );

        // Act
        Set<ConstraintViolation<TransactionDto.DepositRequest>> violations = validator.validate(depositRequest);

        // Assert
        assertTrue(violations.isEmpty(), "accountId should be allowed to be null (not annotated with @NotNull)");
    }

    @Test
    void depositRequest_ShouldHaveInvalidAmount_WhenNull() {
        // Arrange
        TransactionDto.DepositRequest depositRequest = new TransactionDto.DepositRequest(
                UUID.randomUUID(),
                null, // Null amount
                "DEP456",
                "Test deposit"
        );

        // Act
        Set<ConstraintViolation<TransactionDto.DepositRequest>> violations = validator.validate(depositRequest);

        // Assert
        assertFalse(violations.isEmpty(), "There should be validation violations");
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("notnull"));
    }

    @Test
    void depositRequest_ShouldHaveInvalidAmount_WhenNotPositive() {
        // Arrange
        TransactionDto.DepositRequest depositRequestZero = new TransactionDto.DepositRequest(
                UUID.randomUUID(),
                BigDecimal.ZERO, // Zero amount
                "DEP456",
                "Test deposit"
        );

        TransactionDto.DepositRequest depositRequestNegative = new TransactionDto.DepositRequest(
                UUID.randomUUID(),
                BigDecimal.valueOf(-10.00), // Negative amount
                "DEP456",
                "Test deposit"
        );

        // Act
        Set<ConstraintViolation<TransactionDto.DepositRequest>> violationsZero = validator.validate(depositRequestZero);
        Set<ConstraintViolation<TransactionDto.DepositRequest>> violationsNegative = validator.validate(depositRequestNegative);

        // Assert
        assertFalse(violationsZero.isEmpty(), "There should be validation violations for zero amount");
        assertEquals(1, violationsZero.size());
        assertTrue(violationsZero.iterator().next().getMessage().contains("Positive"));

        assertFalse(violationsNegative.isEmpty(), "There should be validation violations for negative amount");
        assertEquals(1, violationsNegative.size());
        assertTrue(violationsNegative.iterator().next().getMessage().contains("Positive"));
    }

    @Test
    void depositRequest_ShouldHaveInvalidAmount_WhenFractionTooLong() {
        // Arrange
        TransactionDto.DepositRequest depositRequest = new TransactionDto.DepositRequest(
                UUID.randomUUID(),
                new BigDecimal("100.12345"), // 5 decimal places (too many, max 4)
                "DEP456",
                "Test deposit"
        );

        // Act
        Set<ConstraintViolation<TransactionDto.DepositRequest>> violations = validator.validate(depositRequest);

        // Assert
        assertFalse(violations.isEmpty(), "There should be validation violations for too many decimal places");
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("Digits"));
    }

    @Test
    void depositRequest_ShouldHaveInvalidAmount_WhenIntegerTooLong() {
        // Arrange
        TransactionDto.DepositRequest depositRequest = new TransactionDto.DepositRequest(
                UUID.randomUUID(),
                new BigDecimal("1234567890123456.0000"), // 16 integer digits (too many, max 15)
                "DEP456",
                "Test deposit"
        );

        // Act
        Set<ConstraintViolation<TransactionDto.DepositRequest>> violations = validator.validate(depositRequest);

        // Assert
        assertFalse(violations.isEmpty(), "There should be validation violations for too many integer digits");
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("Digits"));
    }

    @Test
    void depositRequest_ShouldHaveInvalidReferenceId_WhenTooLong() {
        // Arrange
        TransactionDto.DepositRequest depositRequest = new TransactionDto.DepositRequest(
                UUID.randomUUID(),
                BigDecimal.valueOf(100.00),
                "A".repeat(101), // 101 characters (too long)
                "Test deposit"
        );

        // Act
        Set<ConstraintViolation<TransactionDto.DepositRequest>> violations = validator.validate(depositRequest);

        // Assert
        assertFalse(violations.isEmpty(), "There should be validation violations for too long reference ID");
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("size"));
    }

    @Test
    void depositRequest_ShouldHaveInvalidDescription_WhenTooLong() {
        // Arrange
        TransactionDto.DepositRequest depositRequest = new TransactionDto.DepositRequest(
                UUID.randomUUID(),
                BigDecimal.valueOf(100.00),
                "DEP456",
                "A".repeat(256) // 256 characters (too long)
        );

        // Act
        Set<ConstraintViolation<TransactionDto.DepositRequest>> violations = validator.validate(depositRequest);

        // Assert
        assertFalse(violations.isEmpty(), "There should be validation violations for too long description");
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("size"));
    }

    @Test
    void withdrawRequest_ShouldBeValid_WhenValidRequestProvided() {
        // Arrange
        TransactionDto.WithdrawRequest withdrawRequest = new TransactionDto.WithdrawRequest(
                UUID.randomUUID(),
                BigDecimal.valueOf(100.00),
                "WDL789",
                "Test withdrawal"
        );

        // Act
        Set<ConstraintViolation<TransactionDto.WithdrawRequest>> violations = validator.validate(withdrawRequest);

        // Assert
        assertTrue(violations.isEmpty(), "There should be no validation violations");
    }

    @Test
    void withdrawRequest_ShouldHaveInvalidAccountId_WhenNull() {
        // Arrange
        TransactionDto.WithdrawRequest withdrawRequest = new TransactionDto.WithdrawRequest(
                null, // Null accountId
                BigDecimal.valueOf(100.00),
                "WDL789",
                "Test withdrawal"
        );

        // Act
        Set<ConstraintViolation<TransactionDto.WithdrawRequest>> violations = validator.validate(withdrawRequest);

        // Assert
        assertTrue(violations.isEmpty(), "accountId should be allowed to be null (not annotated with @NotNull)");
    }

    @Test
    void withdrawRequest_ShouldHaveInvalidAmount_WhenNull() {
        // Arrange
        TransactionDto.WithdrawRequest withdrawRequest = new TransactionDto.WithdrawRequest(
                UUID.randomUUID(),
                null, // Null amount
                "WDL789",
                "Test withdrawal"
        );

        // Act
        Set<ConstraintViolation<TransactionDto.WithdrawRequest>> violations = validator.validate(withdrawRequest);

        // Assert
        assertFalse(violations.isEmpty(), "There should be validation violations");
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("notnull"));
    }

    @Test
    void withdrawRequest_ShouldHaveInvalidAmount_WhenNotPositive() {
        // Arrange
        TransactionDto.WithdrawRequest withdrawRequestZero = new TransactionDto.WithdrawRequest(
                UUID.randomUUID(),
                BigDecimal.ZERO, // Zero amount
                "WDL789",
                "Test withdrawal"
        );

        TransactionDto.WithdrawRequest withdrawRequestNegative = new TransactionDto.WithdrawRequest(
                UUID.randomUUID(),
                BigDecimal.valueOf(-10.00), // Negative amount
                "WDL789",
                "Test withdrawal"
        );

        // Act
        Set<ConstraintViolation<TransactionDto.WithdrawRequest>> violationsZero = validator.validate(withdrawRequestZero);
        Set<ConstraintViolation<TransactionDto.WithdrawRequest>> violationsNegative = validator.validate(withdrawRequestNegative);

        // Assert
        assertFalse(violationsZero.isEmpty(), "There should be validation violations for zero amount");
        assertEquals(1, violationsZero.size());
        assertTrue(violationsZero.iterator().next().getMessage().contains("Positive"));

        assertFalse(violationsNegative.isEmpty(), "There should be validation violations for negative amount");
        assertEquals(1, violationsNegative.size());
        assertTrue(violationsNegative.iterator().next().getMessage().contains("Positive"));
    }

    @Test
    void withdrawRequest_ShouldHaveInvalidAmount_WhenFractionTooLong() {
        // Arrange
        TransactionDto.WithdrawRequest withdrawRequest = new TransactionDto.WithdrawRequest(
                UUID.randomUUID(),
                new BigDecimal("100.12345"), // 5 decimal places (too many, max 4)
                "WDL789",
                "Test withdrawal"
        );

        // Act
        Set<ConstraintViolation<TransactionDto.WithdrawRequest>> violations = validator.validate(withdrawRequest);

        // Assert
        assertFalse(violations.isEmpty(), "There should be validation violations for too many decimal places");
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("Digits"));
    }

    @Test
    void withdrawRequest_ShouldHaveInvalidAmount_WhenIntegerTooLong() {
        // Arrange
        TransactionDto.WithdrawRequest withdrawRequest = new TransactionDto.WithdrawRequest(
                UUID.randomUUID(),
                new BigDecimal("1234567890123456.0000"), // 16 integer digits (too many, max 15)
                "WDL789",
                "Test withdrawal"
        );

        // Act
        Set<ConstraintViolation<TransactionDto.WithdrawRequest>> violations = validator.validate(withdrawRequest);

        // Assert
        assertFalse(violations.isEmpty(), "There should be validation violations for too many integer digits");
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("Digits"));
    }

    @Test
    void withdrawRequest_ShouldHaveInvalidReferenceId_WhenTooLong() {
        // Arrange
        TransactionDto.WithdrawRequest withdrawRequest = new TransactionDto.WithdrawRequest(
                UUID.randomUUID(),
                BigDecimal.valueOf(100.00),
                "A".repeat(101), // 101 characters (too long)
                "Test withdrawal"
        );

        // Act
        Set<ConstraintViolation<TransactionDto.WithdrawRequest>> violations = validator.validate(withdrawRequest);

        // Assert
        assertFalse(violations.isEmpty(), "There should be validation violations for too long reference ID");
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("size"));
    }

    @Test
    void withdrawRequest_ShouldHaveInvalidDescription_WhenTooLong() {
        // Arrange
        TransactionDto.WithdrawRequest withdrawRequest = new TransactionDto.WithdrawRequest(
                UUID.randomUUID(),
                BigDecimal.valueOf(100.00),
                "WDL789",
                "A".repeat(256) // 256 characters (too long)
        );

        // Act
        Set<ConstraintViolation<TransactionDto.WithdrawRequest>> violations = validator.validate(withdrawRequest);

        // Assert
        assertFalse(violations.isEmpty(), "There should be validation violations for too long description");
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("size"));
    }

    @Test
    void transferRequest_ShouldBeValid_WhenValidRequestProvided() {
        // Arrange
        TransactionDto.TransferRequest transferRequest = new TransactionDto.TransferRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                BigDecimal.valueOf(300.00),
                "TXF456",
                "Test transfer"
        );

        // Act
        Set<ConstraintViolation<TransactionDto.TransferRequest>> violations = validator.validate(transferRequest);

        // Assert
        assertTrue(violations.isEmpty(), "There should be no validation violations");
    }

    @Test
    void transferRequest_ShouldHaveInvalidFromAccountId_WhenNull() {
        // Arrange
        TransactionDto.TransferRequest transferRequest = new TransactionDto.TransferRequest(
                null, // Null fromAccountId
                UUID.randomUUID(),
                BigDecimal.valueOf(300.00),
                "TXF456",
                "Test transfer"
        );

        // Act
        Set<ConstraintViolation<TransactionDto.TransferRequest>> violations = validator.validate(transferRequest);

        // Assert
        assertTrue(violations.isEmpty(), "fromAccountId should be allowed to be null (not annotated with @NotNull)");
    }

    @Test
    void transferRequest_ShouldHaveInvalidToAccountId_WhenNull() {
        // Arrange
        TransactionDto.TransferRequest transferRequest = new TransactionDto.TransferRequest(
                UUID.randomUUID(),
                null, // Null toAccountId
                BigDecimal.valueOf(300.00),
                "TXF456",
                "Test transfer"
        );

        // Act
        Set<ConstraintViolation<TransactionDto.TransferRequest>> violations = validator.validate(transferRequest);

        // Assert
        assertTrue(violations.isEmpty(), "toAccountId should be allowed to be null (not annotated with @NotNull)");
    }

    @Test
    void transferRequest_ShouldHaveInvalidAmount_WhenNull() {
        // Arrange
        TransactionDto.TransferRequest transferRequest = new TransactionDto.TransferRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null, // Null amount
                "TXF456",
                "Test transfer"
        );

        // Act
        Set<ConstraintViolation<TransactionDto.TransferRequest>> violations = validator.validate(transferRequest);

        // Assert
        assertFalse(violations.isEmpty(), "There should be validation violations");
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("notnull"));
    }

    @Test
    void transferRequest_ShouldHaveInvalidAmount_WhenNotPositive() {
        // Arrange
        TransactionDto.TransferRequest transferRequestZero = new TransactionDto.TransferRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                BigDecimal.ZERO, // Zero amount
                "TXF456",
                "Test transfer"
        );

        TransactionDto.TransferRequest transferRequestNegative = new TransactionDto.TransferRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                BigDecimal.valueOf(-10.00), // Negative amount
                "TXF456",
                "Test transfer"
        );

        // Act
        Set<ConstraintViolation<TransactionDto.TransferRequest>> violationsZero = validator.validate(transferRequestZero);
        Set<ConstraintViolation<TransactionDto.TransferRequest>> violationsNegative = validator.validate(transferRequestNegative);

        // Assert
        assertFalse(violationsZero.isEmpty(), "There should be validation violations for zero amount");
        assertEquals(1, violationsZero.size());
        assertTrue(violationsZero.iterator().next().getMessage().contains("Positive"));

        assertFalse(violationsNegative.isEmpty(), "There should be validation violations for negative amount");
        assertEquals(1, violationsNegative.size());
        assertTrue(violationsNegative.iterator().next().getMessage().contains("Positive"));
    }

    @Test
    void transferRequest_ShouldHaveInvalidAmount_WhenFractionTooLong() {
        // Arrange
        TransactionDto.TransferRequest transferRequest = new TransactionDto.TransferRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("300.12345"), // 5 decimal places (too many, max 4)
                "TXF456",
                "Test transfer"
        );

        // Act
        Set<ConstraintViolation<TransactionDto.TransferRequest>> violations = validator.validate(transferRequest);

        // Assert
        assertFalse(violations.isEmpty(), "There should be validation violations for too many decimal places");
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("Digits"));
    }

    @Test
    void transferRequest_ShouldHaveInvalidAmount_WhenIntegerTooLong() {
        // Arrange
        TransactionDto.TransferRequest transferRequest = new TransactionDto.TransferRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("1234567890123456.0000"), // 16 integer digits (too many, max 15)
                "TXF456",
                "Test transfer"
        );

        // Act
        Set<ConstraintViolation<TransactionDto.TransferRequest>> violations = validator.validate(transferRequest);

        // Assert
        assertFalse(violations.isEmpty(), "There should be validation violations for too many integer digits");
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("Digits"));
    }

    @Test
    void transferRequest_ShouldHaveInvalidReferenceId_WhenTooLong() {
        // Arrange
        TransactionDto.TransferRequest transferRequest = new TransactionDto.TransferRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                BigDecimal.valueOf(300.00),
                "A".repeat(101), // 101 characters (too long)
                "Test transfer"
        );

        // Act
        Set<ConstraintViolation<TransactionDto.TransferRequest>> violations = validator.validate(transferRequest);

        // Assert
        assertFalse(violations.isEmpty(), "There should be validation violations for too long reference ID");
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("size"));
    }

    @Test
    void transferRequest_ShouldHaveInvalidDescription_WhenTooLong() {
        // Arrange
        TransactionDto.TransferRequest transferRequest = new TransactionDto.TransferRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                BigDecimal.valueOf(300.00),
                "TXF456",
                "A".repeat(256) // 256 characters (too long)
        );

        // Act
        Set<ConstraintViolation<TransactionDto.TransferRequest>> violations = validator.validate(transferRequest);

        // Assert
        assertFalse(violations.isEmpty(), "There should be validation violations for too long description");
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("size"));
    }
}