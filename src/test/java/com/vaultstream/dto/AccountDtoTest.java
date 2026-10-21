package com.vaultstream.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class AccountDtoTest {
    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    @Test
    void validAccountDtoPassesValidation() {
        AccountDto dto = AccountDto.builder().accountNumber("1234567890").accountHolderName("Alice Doe")
                .balance(new BigDecimal("10.2500")).build();

        assertTrue(validator.validate(dto).isEmpty());
    }

    @Test
    void accountDtoRequiresAccountNumberHolderAndBalance() {
        AccountDto dto = AccountDto.builder().accountNumber(" ").accountHolderName(null).balance(null).build();

        var violations = validator.validate(dto);

        assertEquals(4, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("accountNumber")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("accountHolderName")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("balance")));
    }

    @Test
    void accountNumberMustBeWithinDeclaredLength() {
        AccountDto tooShort = AccountDto.builder().accountNumber("123").accountHolderName("Alice Doe")
                .balance(BigDecimal.ZERO).build();
        AccountDto tooLong = AccountDto.builder().accountNumber("A".repeat(35)).accountHolderName("Alice Doe")
                .balance(BigDecimal.ZERO).build();

        assertTrue(validator.validate(tooShort).stream().anyMatch(v -> v.getPropertyPath().toString().equals("accountNumber")));
        assertTrue(validator.validate(tooLong).stream().anyMatch(v -> v.getPropertyPath().toString().equals("accountNumber")));
    }

    @Test
    void accountHolderNameMustBeWithinDeclaredLength() {
        AccountDto tooShort = AccountDto.builder().accountNumber("1234567890").accountHolderName("A")
                .balance(BigDecimal.ZERO).build();
        AccountDto tooLong = AccountDto.builder().accountNumber("1234567890").accountHolderName("A".repeat(101))
                .balance(BigDecimal.ZERO).build();

        assertTrue(validator.validate(tooShort).stream().anyMatch(v -> v.getPropertyPath().toString().equals("accountHolderName")));
        assertTrue(validator.validate(tooLong).stream().anyMatch(v -> v.getPropertyPath().toString().equals("accountHolderName")));
    }

    @Test
    void balanceAllowsOnlySupportedPrecision() {
        AccountDto dto = AccountDto.builder().accountNumber("1234567890").accountHolderName("Alice Doe")
                .balance(new BigDecimal("1.12345")).build();

        assertTrue(validator.validate(dto).stream().anyMatch(v -> v.getPropertyPath().toString().equals("balance")));
    }

    @Test
    void createRequestRequiresValidAccountNumberAndHolderName() {
        AccountDto.CreateRequest request = new AccountDto.CreateRequest("123", " ");

        var violations = validator.validate(request);

        assertEquals(3, violations.size());
    }

    @Test
    void depositAndWithdrawRequestsRequirePositiveAmounts() {
        assertFalse(validator.validate(new AccountDto.DepositRequest(new BigDecimal("0.00"))).isEmpty());
        assertFalse(validator.validate(new AccountDto.WithdrawRequest(null)).isEmpty());
        assertTrue(validator.validate(new AccountDto.DepositRequest(new BigDecimal("10.1234"))).isEmpty());
    }
}
