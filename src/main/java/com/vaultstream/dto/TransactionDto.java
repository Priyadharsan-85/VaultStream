package com.vaultstream.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionDto {

    private UUID id;

    private UUID accountId;

    @NotNull
    @Digits(integer = 15, fraction = 4)
    private BigDecimal amount;

    @NotNull
    private TransactionType type;

    @Size(max = 100)
    private String referenceId;

    @Size(max = 255)
    private String description;

    @NotNull
    private LocalDateTime timestamp;

    public enum TransactionType {
        DEPOSIT,
        WITHDRAWAL,
        TRANSFER_IN,
        TRANSFER_OUT,
        FEE,
        INTEREST
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepositRequest {
        @NotNull
        private UUID accountId;
        @NotNull
        @Positive
        @Digits(integer = 15, fraction = 4)
        private BigDecimal amount;
        @Size(max = 100)
        private String referenceId;
        @Size(max = 255)
        private String description;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WithdrawRequest {
        @NotNull
        private UUID accountId;
        @NotNull
        @Positive
        @Digits(integer = 15, fraction = 4)
        private BigDecimal amount;
        @Size(max = 100)
        private String referenceId;
        @Size(max = 255)
        private String description;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransferRequest {
        @NotNull
        private UUID fromAccountId;
        @NotNull
        private UUID toAccountId;
        @NotNull
        @Positive
        @Digits(integer = 15, fraction = 4)
        private BigDecimal amount;
        @Size(max = 100)
        private String referenceId;
        @Size(max = 255)
        private String description;
    }
}