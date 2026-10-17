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
public class AccountDto {

    private UUID id;

    @NotBlank
    @Size(min = 10, max = 34)
    private String accountNumber;

    @NotBlank
    @Size(min = 2, max = 100)
    private String accountHolderName;

    @NotNull
    @Digits(integer = 15, fraction = 4)
    private BigDecimal balance;

    private AccountStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public enum AccountStatus {
        ACTIVE,
        SUSPENDED,
        CLOSED,
        PENDING_VERIFICATION
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepositRequest {
        @NotNull
        @Positive
        @Digits(integer = 15, fraction = 4)
        private BigDecimal amount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WithdrawRequest {
        @NotNull
        @Positive
        @Digits(integer = 15, fraction = 4)
        private BigDecimal amount;
    }
}