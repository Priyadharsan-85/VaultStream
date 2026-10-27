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
public class ReconciliationReportDto {

    private LocalDateTime reconciliationTimestamp;
    private BigDecimal totalDebits;
    private BigDecimal totalCredits;
    private BigDecimal systemImbalance;
    private boolean systemBalanced;
    private int accountsAudited;
    private int discrepanciesCount;
    private List<AccountDiscrepancyDto> discrepancies;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AccountDiscrepancyDto {
        private UUID accountId;
        private String accountNumber;
        private BigDecimal accountBalance;
        private BigDecimal ledgerCalculatedBalance;
        private BigDecimal difference;
    }
}
