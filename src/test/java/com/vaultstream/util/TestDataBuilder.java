package com.vaultstream.util;

import com.vaultstream.dto.AccountDto;
import com.vaultstream.dto.TransactionDto;
import com.vaultstream.model.Account;
import com.vaultstream.model.Transaction;
import com.vaultstream.model.Account.AccountStatus;
import com.vaultstream.model.Transaction.TransactionType;

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
                .status(AccountStatus.ACTIVE);
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
        return TransactionDto.DepositRequest.builder()
                .accountId(accountId)
                .amount(BigDecimal.valueOf(100.00))
                .referenceId("DEP-" + UUID.randomUUID().toString().substring(0, 8))
                .description("Test deposit")
                .build();
    }

    public static TransactionDto.WithdrawRequest withdrawRequest(UUID accountId) {
        return TransactionDto.WithdrawRequest.builder()
                .accountId(accountId)
                .amount(BigDecimal.valueOf(50.00))
                .referenceId("WDL-" + UUID.randomUUID().toString().substring(0, 8))
                .description("Test withdrawal")
                .build();
    }

    public static TransactionDto.TransferRequest transferRequest(UUID fromAccountId, UUID toAccountId) {
        return TransactionDto.TransferRequest.builder()
                .fromAccountId(fromAccountId)
                .toAccountId(toAccountId)
                .amount(BigDecimal.valueOf(75.00))
                .referenceId("XFER-" + UUID.randomUUID().toString().substring(0, 8))
                .description("Test transfer")
                .build();
    }

    public static AccountDto.DepositRequest accountDepositRequest(UUID accountId) {
        return AccountDto.DepositRequest.builder()
                .amount(BigDecimal.valueOf(100.00))
                .referenceId("ACC-DEP-" + UUID.randomUUID().toString().substring(0, 8))
                .description("Test account deposit")
                .build();
    }

    public static AccountDto.WithdrawRequest accountWithdrawRequest(UUID accountId) {
        return AccountDto.WithdrawRequest.builder()
                .amount(BigDecimal.valueOf(50.00))
                .referenceId("ACC-WDL-" + UUID.randomUUID().toString().substring(0, 8))
                .description("Test account withdrawal")
                .build();
    }
}