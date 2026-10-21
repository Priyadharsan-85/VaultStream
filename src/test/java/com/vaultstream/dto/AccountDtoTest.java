package com.vaultstream.dto;

import com.vaultstream.dto.AccountDto.AccountStatus;
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

class AccountDtoTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void accountDto_ShouldBeValid_WhenAllFieldsAreValid() {
        // Arrange
        AccountDto accountDto = AccountDto.builder()
                .id(UUID.randomUUID())
                .accountNumber("TEST12345678901234567890123456789012") // 34 chars (max)
                .accountHolderName("Test User")
                .balance(BigDecimal.valueOf(1000.00))
                .status(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Act
        Set<ConstraintViolation<AccountDto>> violations = validator.validate(accountDto);

        // Assert
        assertTrue(violations.isEmpty(), "There should be no validation violations");
    }

    @Test
    void accountDto_ShouldHaveInvalidAccountNumber_WhenTooShort() {
        // Arrange
        AccountDto accountDto = AccountDto.builder()
                .id(UUID.randomUUID())
                .accountNumber("SHORT") // Too short
                .accountHolderName("Test User")
                .balance(BigDecimal.valueOf(1000.00))
                .status(AccountStatus.ACTIVE)
                .build();

        // Act
        Set<ConstraintViolation<AccountDto>> violations = validator.validate(accountDto);

        // Assert
        assertFalse(violations.isEmpty(), "There should be validation violations");
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("size"));
    }

    @Test
    void accountDto_ShouldHaveInvalidAccountNumber_WhenTooLong() {
        // Arrange
        AccountDto accountDto = AccountDto.builder()
                .id(UUID.randomUUID())
                .accountNumber("TEST123456789012345678901234567890123") // 35 chars (too long)
                .accountHolderName("Test User")
                .balance(BigDecimal.valueOf(1000.00))
                .status(AccountStatus.ACTIVE)
                .build();

        // Act
        Set<ConstraintViolation<AccountDto>> violations = validator.validate(accountDto);

        // Assert
        assertFalse(violations.isEmpty(), "There should be validation violations");
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("size"));
    }

    @Test
    void accountDto_ShouldHaveInvalidAccountNumber_WhenBlank() {
        // Arrange
        AccountDto accountDto = AccountDto.builder()
                .id(UUID.randomUUID())
                .accountNumber("") // Blank
                .accountHolderName("Test User")
                .balance(BigDecimal.valueOf(1000.00))
                .status(AccountStatus.ACTIVE)
                .build();

        // Act
        Set<ConstraintViolation<AccountDto>> violations = validator.validate(accountDto);

        // Assert
        assertFalse(violations.isEmpty(), "There should be validation violations");
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("blank") ||
                   violations.iterator().next().getMessage().contains("empty"));
    }

    @Test
    void accountDto_ShouldHaveInvalidAccountNumber_WhenNull() {
        // Arrange
        AccountDto accountDto = AccountDto.builder()
                .id(UUID.randomUUID())
                .accountNumber(null) // Null
                .accountHolderName("Test User")
                .balance(BigDecimal.valueOf(1000.00))
                .status(AccountStatus.ACTIVE)
                .build();

        // Act
        Set<ConstraintViolation<AccountDto>> violations = validator.validate(accountDto);

        // Assert
        assertFalse(violations.isEmpty(), "There should be validation violations");
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("notnull"));
    }

    @Test
    void accountDto_ShouldHaveInvalidAccountHolderName_WhenTooShort() {
        // Arrange
        AccountDto accountDto = AccountDto.builder()
                .id(UUID.randomUUID())
                .accountNumber("TEST12345678901234567890123456789012")
                .accountHolderName("A") // Too short (min 2)
                .balance(BigDecimal.valueOf(1000.00))
                .status(AccountStatus.ACTIVE)
                .build();

        // Act
        Set<ConstraintViolation<AccountDto>> violations = validator.validate(accountDto);

        // Assert
        assertFalse(violations.isEmpty(), "There should be validation violations");
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("size"));
    }

    @Test
    void accountDto_ShouldHaveInvalidAccountHolderName_WhenTooLong() {
        // Arrange
        String longName = "A".repeat(101); // 101 chars (too long, max 100)
        AccountDto accountDto = AccountDto.builder()
                .id(UUID.randomUUID())
                .accountNumber("TEST12345678901234567890123456789012")
                .accountHolderName(longName)
                .balance(BigDecimal.valueOf(1000.00))
                .status(AccountStatus.ACTIVE)
                .build();

        // Act
        Set<ConstraintViolation<AccountDto>> violations = validator.validate(accountDto);

        // Assert
        assertFalse(violations.isEmpty(), "There should be validation violations");
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("size"));
    }

    @Test
    void accountDto_ShouldHaveInvalidAccountHolderName_WhenBlank() {
        // Arrange
        AccountDto accountDto = AccountDto.builder()
                .id(UUID.randomUUID())
                .accountNumber("TEST12345678901234567890123456789012")
                .accountHolderName("") // Blank
                .balance(BigDecimal.valueOf(1000.00))
                .status(AccountStatus.ACTIVE)
                .build();

        // Act
        Set<ConstraintViolation<AccountDto>> violations = validator.validate(accountDto);

        // Assert
        assertFalse(violations.isEmpty(), "There should be validation violations");
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("blank") ||
                   violations.iterator().next().getMessage().contains("empty"));
    }

    @Test
    void accountDto_ShouldHaveInvalidAccountHolderName_WhenNull() {
        // Arrange
        AccountDto accountDto = AccountDto.builder()
                .id(UUID.randomUUID())
                .accountNumber("TEST12345678901234567890123456789012")
                .accountHolderName(null) // Null
                .balance(BigDecimal.valueOf(1000.00))
                .status(AccountStatus.ACTIVE)
                .build();

        // Act
        Set<ConstraintViolation<AccountDto>> violations = validator.validate(accountDto);

        // Assert
        assertFalse(violations.isEmpty(), "There should be validation violations");
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("notnull"));
    }

    @Test
    void accountDto_ShouldHaveInvalidBalance_WhenNull() {
        // Arrange
        AccountDto accountDto = AccountDto.builder()
                .id(UUID.randomUUID())
                .accountNumber("TEST12345678901234567890123456789012")
                .accountHolderName("Test User")
                .balance(null) // Null balance
                .status(AccountStatus.ACTIVE)
                .build();

        // Act
        Set<ConstraintViolation<AccountDto>> violations = validator.validate(accountDto);

        // Assert
        assertFalse(violations.isEmpty(), "There should be validation violations");
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("notnull"));
    }

    @Test
    void accountDto_ShouldHaveInvalidBalance_WhenNotPositive() {
        // Arrange
        AccountDto accountDtoZero = AccountDto.builder()
                .id(UUID.randomUUID())
                .accountNumber("TEST12345678901234567890123456789012")
                .accountHolderName("Test User")
                .balance(BigDecimal.ZERO) // Zero balance
                .status(AccountStatus.ACTIVE)
                .build();

        AccountDto accountDtoNegative = AccountDto.builder()
                .id(UUID.randomUUID())
                .accountNumber("TEST12345678901234567890123456789012")
                .accountHolderName("Test User")
                .balance(BigDecimal.valueOf(-10.00)) // Negative balance
                .status(AccountStatus.ACTIVE)
                .build();

        // Act
        Set<ConstraintViolation<AccountDto>> violationsZero = validator.validate(accountDtoZero);
        Set<ConstraintViolation<AccountDto>> violationsNegative = validator.validate(accountDtoNegative);

        // Assert
        assertFalse(violationsZero.isEmpty(), "There should be validation violations for zero balance");
        assertEquals(1, violationsZero.size());
        assertTrue(violationsZero.iterator().next().getMessage().contains("Positive"));

        assertFalse(violationsNegative.isEmpty(), "There should be validation violations for negative balance");
        assertEquals(1, violationsNegative.size());
        assertTrue(violationsNegative.iterator().next().getMessage().contains("Positive"));
    }

    @Test
    void accountDto_ShouldHaveInvalidBalance_WhenFractionTooLong() {
        // Arrange
        AccountDto accountDto = AccountDto.builder()
                .id(UUID.randomUUID())
                .accountNumber("TEST12345678901234567890123456789012")
                .accountHolderName("Test User")
                .balance(new BigDecimal("1000.12345")) // 5 decimal places (too many, max 4)
                .status(AccountStatus.ACTIVE)
                .build();

        // Act
        Set<ConstraintViolation<AccountDto>> violations = validator.validate(accountDto);

        // Assert
        assertFalse(violations.isEmpty(), "There should be validation violations for too many decimal places");
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("Digits"));
    }

    @Test
    void accountDto_ShouldHaveInvalidBalance_WhenIntegerTooLong() {
        // Arrange
        AccountDto accountDto = AccountDto.builder()
                .id(UUID.randomUUID())
                .accountNumber("TEST12345678901234567890123456789012")
                .accountHolderName("Test User")
                .balance(new BigDecimal("1234567890123456.0000")) // 16 integer digits (too many, max 15)
                .status(AccountStatus.ACTIVE)
                .build();

        // Act
        Set<ConstraintViolation<AccountDto>> violations = validator.validate(accountDto);

        // Assert
        assertFalse(violations.isEmpty(), "There should be validation violations for too many integer digits");
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("Digits"));
    }

    @Test
    void accountDto_ShouldHaveValidStatusValues() {
        // Arrange & Act & Assert
        for (AccountStatus status : AccountStatus.values()) {
            AccountDto accountDto = AccountDto.builder()
                    .id(UUID.randomUUID())
                    .accountNumber("TEST12345678901234567890123456789012")
                    .accountHolderName("Test User")
                    .balance(BigDecimal.valueOf(1000.00))
                    .status(status)
                    .build();

            Set<ConstraintViolation<AccountDto>> violations = validator.validate(accountDto);
            assertTrue(violations.isEmpty(), "Status " + status + " should be valid");
        }
    }

    @Test
    void accountDto_ShouldAllowNullStatus() {
        // Arrange
        AccountDto accountDto = AccountDto.builder()
                .id(UUID.randomUUID())
                .accountNumber("TEST12345678901234567890123456789012")
                .accountHolderName("Test User")
                .balance(BigDecimal.valueOf(1000.00))
                .status(null) // Null status should be allowed (not annotated with @NotNull)
                .build();

        // Act
        Set<ConstraintViolation<AccountDto>> violations = validator.validate(accountDto);

        // Assert
        assertTrue(violations.isEmpty(), "Null status should be allowed");
    }

    @Test
    void accountDto_ShouldAllowNullTimestamps() {
        // Arrange
        AccountDto accountDto = AccountDto.builder()
                .id(UUID.randomUUID())
                .accountNumber("TEST12345678901234567890123456789012")
                .accountHolderName("Test User")
                .balance(BigDecimal.valueOf(1000.00))
                .status(AccountStatus.ACTIVE)
                .createdAt(null) // Null timestamps should be allowed
                .updatedAt(null)
                .build();

        // Act
        Set<ConstraintViolation<AccountDto>> violations = validator.validate(accountDto);

        // Assert
        assertTrue(violations.isEmpty(), "Null timestamps should be allowed");
    }

    @Test
    void accountDto_Builder_ShouldCreateAccountDtoWithAllFields() {
        // Arrange
        UUID testId = UUID.randomUUID();
        String testAccountNumber = "TEST12345678901234567890123456789012";
        String testAccountHolderName = "Test User";
        BigDecimal testBalance = BigDecimal.valueOf(1500.50);
        AccountStatus testStatus = AccountStatus.SUSPENDED;
        LocalDateTime testCreatedAt = LocalDateTime.now().minusDays(1);
        LocalDateTime testUpdatedAt = LocalDateTime.now();

        // Act
        AccountDto accountDto = AccountDto.builder()
                .id(testId)
                .accountNumber(testAccountNumber)
                .accountHolderName(testAccountHolderName)
                .balance(testBalance)
                .status(testStatus)
                .createdAt(testCreatedAt)
                .updatedAt(testUpdatedAt)
                .build();

        // Assert
        assertEquals(testId, accountDto.getId());
        assertEquals(testAccountNumber, accountDto.getAccountNumber());
        assertEquals(testAccountHolderName, accountDto.getAccountHolderName());
        assertEquals(testBalance, accountDto.getBalance());
        assertEquals(testStatus, accountDto.getStatus());
        assertEquals(testCreatedAt, accountDto.getCreatedAt());
        assertEquals(testUpdatedAt, accountDto.getUpdatedAt());
    }

    @Test
    void depositRequest_ShouldBeValid_WhenValidAmountProvided() {
        // Arrange
        AccountDto.DepositRequest depositRequest = new AccountDto.DepositRequest(
                BigDecimal.valueOf(100.00)
        );

        // Act
        Set<ConstraintViolation<AccountDto.DepositRequest>> violations = validator.validate(depositRequest);

        // Assert
        assertTrue(violations.isEmpty(), "There should be no validation violations");
    }

    @Test
    void depositRequest_ShouldHaveInvalidAmount_WhenNull() {
        // Arrange
        AccountDto.DepositRequest depositRequest = new AccountDto.DepositRequest(
                null // Null amount
        );

        // Act
        Set<ConstraintViolation<AccountDto.DepositRequest>> violations = validator.validate(depositRequest);

        // Assert
        assertFalse(violations.isEmpty(), "There should be validation violations");
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("notnull"));
    }

    @Test
    void depositRequest_ShouldHaveInvalidAmount_WhenNotPositive() {
        // Arrange
        AccountDto.DepositRequest depositRequestZero = new AccountDto.DepositRequest(
                BigDecimal.ZERO // Zero amount
        );

        AccountDto.DepositRequest depositRequestNegative = new AccountDto.DepositRequest(
                BigDecimal.valueOf(-10.00) // Negative amount
        );

        // Act
        Set<ConstraintViolation<AccountDto.DepositRequest>> violationsZero = validator.validate(depositRequestZero);
        Set<ConstraintViolation<AccountDto.DepositRequest>> violationsNegative = validator.validate(depositRequestNegative);

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
        AccountDto.DepositRequest depositRequest = new AccountDto.DepositRequest(
                new BigDecimal("100.12345") // 5 decimal places (too many, max 4)
        );

        // Act
        Set<ConstraintViolation<AccountDto.DepositRequest>> violations = validator.validate(depositRequest);

        // Assert
        assertFalse(violations.isEmpty(), "There should be validation violations for too many decimal places");
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("Digits"));
    }

    @Test
    void depositRequest_ShouldHaveInvalidAmount_WhenIntegerTooLong() {
        // Arrange
        AccountDto.DepositRequest depositRequest = new AccountDto.DepositRequest(
                new BigDecimal("1234567890123456.0000") // 16 integer digits (too many, max 15)
        );

        // Act
        Set<ConstraintViolation<AccountDto.DepositRequest>> violations = validator.validate(depositRequest);

        // Assert
        assertFalse(violations.isEmpty(), "There should be validation violations for too many integer digits");
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("Digits"));
    }

    @Test
    void withdrawRequest_ShouldBeValid_WhenValidAmountProvided() {
        // Arrange
        AccountDto.WithdrawRequest withdrawRequest = new AccountDto.WithdrawRequest(
                BigDecimal.valueOf(100.00)
        );

        // Act
        Set<ConstraintViolation<AccountDto.WithdrawRequest>> violations = validator.validate(withdrawRequest);

        // Assert
        assertTrue(violations.isEmpty(), "There should be no validation violations");
    }

    @Test
    void withdrawRequest_ShouldHaveInvalidAmount_WhenNull() {
        // Arrange
        AccountDto.WithdrawRequest withdrawRequest = new AccountDto.WithdrawRequest(
                null // Null amount
        );

        // Act
        Set<ConstraintViolation<AccountDto.WithdrawRequest>> violations = validator.validate(withdrawRequest);

        // Assert
        assertFalse(violations.isEmpty(), "There should be validation violations");
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("notnull"));
    }

    @Test
    void withdrawRequest_ShouldHaveInvalidAmount_WhenNotPositive() {
        // Arrange
        AccountDto.WithdrawRequest withdrawRequestZero = new AccountDto.WithdrawRequest(
                BigDecimal.ZERO // Zero amount
        );

        AccountDto.WithdrawRequest withdrawRequestNegative = new AccountDto.WithdrawRequest(
                BigDecimal.valueOf(-10.00) // Negative amount
        );

        // Act
        Set<ConstraintViolation<AccountDto.WithdrawRequest>> violationsZero = validator.validate(withdrawRequestZero);
        Set<ConstraintViolation<AccountDto.WithdrawRequest>> violationsNegative = validator.validate(withdrawRequestNegative);

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
        AccountDto.WithdrawRequest withdrawRequest = new AccountDto.WithdrawRequest(
                new BigDecimal("100.12345") // 5 decimal places (too many, max 4)
        );

        // Act
        Set<ConstraintViolation<AccountDto.WithdrawRequest>> violations = validator.validate(withdrawRequest);

        // Assert
        assertFalse(violations.isEmpty(), "There should be validation violations for too many decimal places");
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("Digits"));
    }

    @Test
    void withdrawRequest_ShouldHaveInvalidAmount_WhenIntegerTooLong() {
        // Arrange
        AccountDto.WithdrawRequest withdrawRequest = new AccountDto.WithdrawRequest(
                new BigDecimal("1234567890123456.0000") // 16 integer digits (too many, max 15)
        );

        // Act
        Set<ConstraintViolation<AccountDto.WithdrawRequest>> violations = validator.validate(withdrawRequest);

        // Assert
        assertFalse(violations.isEmpty(), "There should be validation violations for too many integer digits");
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("Digits"));
    }
}
