package com.vaultstream.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionDtoTest {

    private ValidatorFactory validatorFactory;
    private Validator validator;

    @BeforeEach
    void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterEach
    void tearDown() {
        validatorFactory.close();
    }

    @Test
    void transactionDtoCarriesTransactionFields() {
        TransactionDto dto = TransactionDto.builder()
                .id(UUID.randomUUID())
                .accountId(UUID.randomUUID())
                .amount(new BigDecimal("100.25"))
                .type(TransactionDto.TransactionType.DEPOSIT)
                .referenceId("REF-1")
                .description("Deposit")
                .build();

        assertEquals(TransactionDto.TransactionType.DEPOSIT, dto.getType());
        assertEquals(new BigDecimal("100.25"), dto.getAmount());
        assertTrue(validator.validate(dto).isEmpty());
    }

    @Test
    void depositRequestAcceptsValidData() {
        TransactionDto.DepositRequest request = new TransactionDto.DepositRequest(
                UUID.randomUUID(), new BigDecimal("100.25"), "DEP-1", "Deposit");

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void depositRequestRequiresAccountAndPositiveAmount() {
        TransactionDto.DepositRequest request = new TransactionDto.DepositRequest(
                null, BigDecimal.ZERO, null, null);

        Set<ConstraintViolation<TransactionDto.DepositRequest>> violations = validator.validate(request);

        assertHasViolation(violations, "accountId");
        assertHasViolation(violations, "amount");
    }

    @Test
    void depositRequestLimitsAmountPrecisionAndTextLength() {
        TransactionDto.DepositRequest request = new TransactionDto.DepositRequest(
                UUID.randomUUID(), new BigDecimal("1234567890123456.12345"), "R".repeat(101), "D".repeat(256));

        Set<ConstraintViolation<TransactionDto.DepositRequest>> violations = validator.validate(request);

        assertHasViolation(violations, "amount");
        assertHasViolation(violations, "referenceId");
        assertHasViolation(violations, "description");
    }

    @Test
    void withdrawRequestRequiresAccountAndPositiveAmount() {
        TransactionDto.WithdrawRequest request = new TransactionDto.WithdrawRequest(
                null, new BigDecimal("-1.00"), null, null);

        Set<ConstraintViolation<TransactionDto.WithdrawRequest>> violations = validator.validate(request);

        assertHasViolation(violations, "accountId");
        assertHasViolation(violations, "amount");
    }

    @Test
    void transferRequestAcceptsValidData() {
        TransactionDto.TransferRequest request = new TransactionDto.TransferRequest(
                UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("300.00"), "TRF-1", "Transfer");

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void transferRequestRequiresBothAccountsAndPositiveAmount() {
        TransactionDto.TransferRequest request = new TransactionDto.TransferRequest(
                null, null, new BigDecimal("-1.00"), null, null);

        Set<ConstraintViolation<TransactionDto.TransferRequest>> violations = validator.validate(request);

        assertHasViolation(violations, "fromAccountId");
        assertHasViolation(violations, "toAccountId");
        assertHasViolation(violations, "amount");
    }

    @Test
    void transferRequestLimitsAmountPrecisionAndTextLength() {
        TransactionDto.TransferRequest request = new TransactionDto.TransferRequest(
                UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("1234567890123456.12345"),
                "R".repeat(101), "D".repeat(256));

        Set<ConstraintViolation<TransactionDto.TransferRequest>> violations = validator.validate(request);

        assertHasViolation(violations, "amount");
        assertHasViolation(violations, "referenceId");
        assertHasViolation(violations, "description");
    }

    private static void assertHasViolation(Set<? extends ConstraintViolation<?>> violations, String property) {
        assertFalse(violations.stream().noneMatch(v -> v.getPropertyPath().toString().equals(property)),
                "Expected a validation violation for " + property);
    }
}
