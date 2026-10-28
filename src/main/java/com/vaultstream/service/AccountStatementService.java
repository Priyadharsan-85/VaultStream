package com.vaultstream.service;

import com.vaultstream.dto.AccountStatementDto;
import com.vaultstream.dto.TransactionDto;
import com.vaultstream.exception.ResourceNotFoundException;
import com.vaultstream.mapper.TransactionMapper;
import com.vaultstream.model.Account;
import com.vaultstream.model.Transaction;
import com.vaultstream.repository.AccountRepository;
import com.vaultstream.repository.LedgerEntryRepository;
import com.vaultstream.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountStatementService {

    private static final long MAX_STATEMENT_DAYS = 366;

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final TransactionMapper transactionMapper;

    @Transactional(readOnly = true)
    public AccountStatementDto generateStatement(UUID accountId, LocalDateTime startTime, LocalDateTime endTime,
                                                  String ownerUsername) {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("startTime and endTime must not be null");
        }
        if (endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("endTime must be equal to or later than startTime");
        }
        if (Duration.between(startTime, endTime).toDays() > MAX_STATEMENT_DAYS) {
            throw new IllegalArgumentException("Statement period cannot exceed " + MAX_STATEMENT_DAYS + " days");
        }

        Account account = isCallerAdmin()
                ? accountRepository.findById(accountId)
                        .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId))
                : accountRepository.findByIdAndOwnerUser_Username(accountId, ownerUsername)
                        .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));

        BigDecimal openingBalance = ledgerEntryRepository.calculateLedgerBalanceBefore(accountId, startTime);
        List<Transaction> transactions = transactionRepository.findAllByAccountIdAndTimeRange(accountId, startTime, endTime);

        BigDecimal totalDeposits = BigDecimal.ZERO;
        BigDecimal totalWithdrawals = BigDecimal.ZERO;
        BigDecimal totalTransfersIn = BigDecimal.ZERO;
        BigDecimal totalTransfersOut = BigDecimal.ZERO;

        for (Transaction tx : transactions) {
            switch (tx.getType()) {
                case DEPOSIT -> totalDeposits = totalDeposits.add(tx.getAmount());
                case WITHDRAWAL, FEE -> totalWithdrawals = totalWithdrawals.add(tx.getAmount());
                case TRANSFER_IN, INTEREST -> totalTransfersIn = totalTransfersIn.add(tx.getAmount());
                case TRANSFER_OUT -> totalTransfersOut = totalTransfersOut.add(tx.getAmount());
            }
        }

        BigDecimal totalCredits = totalDeposits.add(totalTransfersIn);
        BigDecimal totalDebits = totalWithdrawals.add(totalTransfersOut);
        BigDecimal netCashFlow = totalCredits.subtract(totalDebits);
        BigDecimal closingBalance = openingBalance.add(netCashFlow);

        List<TransactionDto> transactionDtos = transactions.stream()
                .map(transactionMapper::toDto)
                .toList();

        return AccountStatementDto.builder()
                .accountId(account.getId())
                .accountNumber(account.getAccountNumber())
                .accountHolderName(account.getAccountHolderName())
                .periodStart(startTime)
                .periodEnd(endTime)
                .openingBalance(openingBalance)
                .closingBalance(closingBalance)
                .netCashFlow(netCashFlow)
                .totalDeposits(totalDeposits)
                .totalWithdrawals(totalWithdrawals)
                .totalTransfersIn(totalTransfersIn)
                .totalTransfersOut(totalTransfersOut)
                .totalTransactions(transactions.size())
                .transactions(transactionDtos)
                .build();
    }

    private boolean isCallerAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
