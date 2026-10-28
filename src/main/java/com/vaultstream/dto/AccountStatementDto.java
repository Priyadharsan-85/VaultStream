package com.vaultstream.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountStatementDto {

    private UUID accountId;
    private String accountNumber;
    private String accountHolderName;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private BigDecimal openingBalance;
    private BigDecimal closingBalance;
    private BigDecimal netCashFlow;
    private BigDecimal totalDeposits;
    private BigDecimal totalWithdrawals;
    private BigDecimal totalTransfersIn;
    private BigDecimal totalTransfersOut;
    private int totalTransactions;
    private List<TransactionDto> transactions;
}
