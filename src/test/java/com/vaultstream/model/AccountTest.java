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

class AccountTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void account_ShouldBeValid_WhenAllFieldsAreValid() {
        // Arrange
        Account account = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber("TEST12345678901234567890123456789012") // 34 chars (max)
                .accountHolderName("Test User")
                .balance(BigDecimal.valueOf(1000.00))
                .status(Account.AccountStatus.ACTIVE)
                .build();

        // Act
        Set<ConstraintViolation<Account>> violations = validator.validate(account);

        // Assert
        assertTrue(violations.isEmpty(), "There should be no validation violations");
    }

    @Test
    void account_ShouldHaveInvalidAccountNumber_WhenTooShort() {
        // Arrange
        Account account = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber("SHORT") // Too short
                .accountHolderName("Test User")
                .balance(BigDecimal.valueOf(1000.00))
                .status(Account.AccountStatus.ACTIVE)
                .build();

        // Act
        Set<ConstraintViolation<Account>> violations = validator.validate(account);

        // Assert
        assertFalse(violations.isEmpty(), "There should be validation violations");
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("size"));
    }

    @Test
    void account_ShouldHaveInvalidAccountNumber_WhenTooLong() {
        // Arrange
        Account account = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber("TEST123456789012345678901234567890123") // 35 chars (too long)
                .accountHolderName("Test User")
                .balance(BigDecimal.valueOf(1000.00))
                .status(Account.AccountStatus.ACTIVE)
                .build();

        // Act
        Set<ConstraintViolation<Account>> violations = validator.validate(account);

        // Assert
        assertFalse(violations.isEmpty(), "There should be validation violations");
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("size"));
    }

    @Test
    void account_ShouldHaveInvalidAccountHolderName_WhenTooShort() {
        // Arrange
        Account account = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber("TEST12345678901234567890123456789012")
                .accountHolderName("A") // Too short (min 2)
                .balance(BigDecimal.valueOf(1000.00))
                .status(Account.AccountStatus.ACTIVE)
                .build();

        // Act
        Set<ConstraintViolation<Account>> violations = validator.validate(account);

        // Assert
        assertFalse(violations.isEmpty(), "There should be validation violations");
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("size"));
    }

    @Test
    void account_ShouldHaveInvalidAccountHolderName_WhenTooLong() {
        // Arrange
        String longName = "A".repeat(101); // 101 chars (too long, max 100)
        Account account = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber("TEST12345678901234567890123456789012")
                .accountHolderName(longName)
                .balance(BigDecimal.valueOf(1000.00))
                .status(Account.AccountStatus.ACTIVE)
                .build();

        // Act
        Set<ConstraintViolation<Account>> violations = validator.validate(account);

        // Assert
        assertFalse(violations.isEmpty(), "There should be validation violations");
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("size"));
    }

    @Test
    void account_ShouldHaveInvalidBalance_WhenNull() {
        // Arrange
        Account account = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber("TEST12345678901234567890123456789012")
                .accountHolderName("Test User")
                .balance(null) // Null balance
                .status(Account.AccountStatus.ACTIVE)
                .build();

        // Act
        Set<ConstraintViolation<Account>> violations = validator.validate(account);

        // Assert
        assertFalse(violations.isEmpty(), "There should be validation violations");
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("notnull"));
    }

    @Test
    void account_ShouldHaveInvalidStatus_WhenNull() {
        // Arrange
        Account account = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber("TEST12345678901234567890123456789012")
                .accountHolderName("Test User")
                .balance(BigDecimal.valueOf(1000.00))
                .status(null) // Null status
                .build();

        // Act
        Set<ConstraintViolation<Account>> violations = validator.validate(account);

        // Assert
        assertFalse(violations.isEmpty(), "There should be validation violations");
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("notnull"));
    }

    @Test
    void account_ShouldHaveValidStatusValues() {
        // Arrange & Act & Assert
        for (Account.AccountStatus status : Account.AccountStatus.values()) {
            Account account = Account.builder()
                    .id(UUID.randomUUID())
                    .accountNumber("TEST12345678901234567890123456789012")
                    .accountHolderName("Test User")
                    .balance(BigDecimal.valueOf(1000.00))
                    .status(status)
                    .build();

            Set<ConstraintViolation<Account>> violations = validator.validate(account);
            assertTrue(violations.isEmpty(), "Status " + status + " should be valid");
        }
    }

    @Test
    void account_ShouldSetTimestamps_OnCreate() {
        // Arrange
        Account account = new Account();
        account.setAccountNumber("TEST12345678901234567890123456789012");
        account.setAccountHolderName("Test User");
        account.setBalance(BigDecimal.valueOf(1000.00));
        account.setStatus(Account.AccountStatus.ACTIVE);

        // Act
        LocalDateTime beforeCreate = LocalDateTime.now();
        // Simulate @PrePersist by calling onCreate directly (normally called by Hibernate)
        account.onCreate();
        LocalDateTime afterCreate = LocalDateTime.now();

        // Assert
        assertNotNull(account.getCreatedAt());
        assertNotNull(account.getUpdatedAt());
        assertFalse(account.getCreatedAt().isBefore(beforeCreate));
        assertFalse(account.getCreatedAt().isAfter(afterCreate));
        assertFalse(account.getUpdatedAt().isBefore(beforeCreate));
        assertFalse(account.getUpdatedAt().isAfter(afterCreate));
        // Initially createdAt and updatedAt should be equal
        assertEquals(account.getCreatedAt(), account.getUpdatedAt());
    }

    @Test
    void account_ShouldUpdateUpdatedAt_OnUpdate() {
        // Arrange
        Account account = new Account();
        account.setId(UUID.randomUUID());
        account.setAccountNumber("TEST12345678901234567890123456789012");
        account.setAccountHolderName("Test User");
        account.setBalance(BigDecimal.valueOf(1000.00));
        account.setStatus(Account.AccountStatus.ACTIVE);

        // Set initial timestamps
        LocalDateTime oldTime = LocalDateTime.now().minusMinutes(10);
        account.setCreatedAt(oldTime);
        account.setUpdatedAt(oldTime);

        // Act
        LocalDateTime beforeUpdate = LocalDateTime.now();
        // Simulate @PreUpdate by calling onUpdate directly
        account.onUpdate();
        LocalDateTime afterUpdate = LocalDateTime.now();

        // Assert
        assertEquals(oldTime, account.getCreatedAt()); // createdAt should not change
        assertFalse(account.getUpdatedAt().isBefore(beforeUpdate));
        assertFalse(account.getUpdatedAt().isAfter(afterUpdate));
        assertTrue(account.getUpdatedAt().isAfter(oldTime)); // updatedAt should be newer than oldTime
    }

    @Test
    void account_Builder_ShouldCreateAccountWithAllFields() {
        // Arrange
        UUID testId = UUID.randomUUID();
        String testAccountNumber = "TEST12345678901234567890123456789012";
        String testAccountHolderName = "Test User";
        BigDecimal testBalance = BigDecimal.valueOf(1500.50);
        Account.AccountStatus testStatus = Account.AccountStatus.SUSPENDED;
        LocalDateTime testCreatedAt = LocalDateTime.now().minusDays(1);
        LocalDateTime testUpdatedAt = LocalDateTime.now();

        // Act
        Account account = Account.builder()
                .id(testId)
                .accountNumber(testAccountNumber)
                .accountHolderName(testAccountHolderName)
                .balance(testBalance)
                .status(testStatus)
                .createdAt(testCreatedAt)
                .updatedAt(testUpdatedAt)
                .build();

        // Assert
        assertEquals(testId, account.getId());
        assertEquals(testAccountNumber, account.getAccountNumber());
        assertEquals(testAccountHolderName, account.getAccountHolderName());
        assertEquals(testBalance, account.getBalance());
        assertEquals(testStatus, account.getStatus());
        assertEquals(testCreatedAt, account.getCreatedAt());
        assertEquals(testUpdatedAt, account.getUpdatedAt());
    }
}