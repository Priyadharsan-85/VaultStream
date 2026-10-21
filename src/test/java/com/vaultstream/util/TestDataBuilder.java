package com.vaultstream.util;

import com.vaultstream.dto.AccountDto;
import com.vaultstream.dto.TransactionDto;
import com.vaultstream.model.Account;
import com.vaultstream.model.Transaction;
import com.vaultstream.model.Account.AccountStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Utility class for creating test data objects.
 */
public class TestDataBuilder {

    private TestDataBuilder() {
        // Utility class - prevent instantiation
    }

    public static AccountDto.AccountDtoBuilder accountDto() {
        return AccountDto.builder()
                .accountNumber("ACC" + UUID.randomUUID().toString().substring(0, 8))
                .accountHolderName("Test Account Holder")
                .balance(BigDecimal.valueOf(1000.00))
                .status(AccountDto.AccountStatus.ACTIVE);
    }

    public static Account.AccountBuilder account() {
        return Account.builder()
                .accountNumber("ACC" + UUID.randomUUID().toString().substring(0, 8))
                .accountHolderName("Test Account Holder")
                .balance(BigDecimal.valueOf(1000.00))
                .status(AccountStatus.ACTIVE);
    }

    public static TransactionDto.TransactionDtoBuilder transactionDto() {
        return TransactionDto.builder()
                .amount(BigDecimal.valueOf(100.00))
                .referenceId("REF-" + UUID.randomUUID().toString().substring(0, 8))
                .description("Test transaction");
    }

    public static Transaction.TransactionBuilder transaction() {
        return Transaction.builder()
                .amount(BigDecimal.valueOf(100.00))
                .referenceId("REF-" + UUID.randomUUID().toString().substring(0, 8))
                .description("Test transaction");
    }

    public static TransactionDto.DepositRequest depositRequest(UUID accountId) {
        return new TransactionDto.DepositRequest(accountId, BigDecimal.valueOf(100.00),
                "DEP-" + UUID.randomUUID().toString().substring(0, 8), "Test deposit");
    }

    public static TransactionDto.WithdrawRequest withdrawRequest(UUID accountId) {
        return new TransactionDto.WithdrawRequest(accountId, BigDecimal.valueOf(50.00),
                "WDL-" + UUID.randomUUID().toString().substring(0, 8), "Test withdrawal");
    }

    public static TransactionDto.TransferRequest transferRequest(UUID fromAccountId, UUID toAccountId) {
        return new TransactionDto.TransferRequest(fromAccountId, toAccountId, BigDecimal.valueOf(75.00),
                "XFER-" + UUID.randomUUID().toString().substring(0, 8), "Test transfer");
    }

    public static AccountDto.DepositRequest accountDepositRequest(UUID accountId) {
        return new AccountDto.DepositRequest(BigDecimal.valueOf(100.00));
    }

    public static AccountDto.WithdrawRequest accountWithdrawRequest(UUID accountId) {
        return new AccountDto.WithdrawRequest(BigDecimal.valueOf(50.00));
    }
}
