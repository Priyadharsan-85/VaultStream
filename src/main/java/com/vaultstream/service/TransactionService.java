package com.vaultstream.service;

import com.vaultstream.dto.TransactionDto;
import com.vaultstream.exception.ResourceNotFoundException;
import com.vaultstream.mapper.TransactionMapper;
import com.vaultstream.model.Account;
import com.vaultstream.model.LedgerEntry;
import com.vaultstream.model.LedgerJournal;
import com.vaultstream.model.Transaction;
import com.vaultstream.model.Transaction.TransactionType;
import com.vaultstream.monitoring.TransactionMetrics;
import com.vaultstream.repository.AccountRepository;
import com.vaultstream.repository.LedgerEntryRepository;
import com.vaultstream.repository.LedgerJournalRepository;
import com.vaultstream.repository.TransactionRepository;
import com.vaultstream.websocket.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final LedgerJournalRepository ledgerJournalRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final TransactionMapper transactionMapper;
    private final TransactionMetrics transactionMetrics;
    private final WebSocketService webSocketService;

    @Value("${vaultstream.ledger.manual-adjustments-enabled:true}")
    private boolean manualAdjustmentsEnabled = true;

    @Value("${vaultstream.ledger.customer-transfers-enabled:true}")
    private boolean customerTransfersEnabled = true;

    @Value("${vaultstream.ledger.max-transfer-amount:50000.00}")
    private BigDecimal maxTransferAmount = new BigDecimal("50000.00");

    @Value("${vaultstream.ledger.daily-transfer-limit:100000.00}")
    private BigDecimal dailyTransferLimit = new BigDecimal("100000.00");

    private static final UUID CLEARING_ACCOUNT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Transactional(readOnly = true)
    public TransactionDto getTransactionById(UUID id, String ownerUsername) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));
        requireAccountOwnership(transaction.getAccountId(), ownerUsername);
        return transactionMapper.toDto(transaction);
    }

    @Transactional(readOnly = true)
    public TransactionDto getTransactionByReferenceId(String referenceId, String ownerUsername) {
        List<Transaction> transactions = transactionRepository.findAllByReferenceId(referenceId);
        if (transactions.isEmpty()) {
            throw new ResourceNotFoundException("Transaction not found with reference id: " + referenceId);
        }

        boolean admin = isCallerAdmin();
        for (Transaction tx : transactions) {
            if (admin || accountRepository.existsByIdAndOwnerUser_Username(tx.getAccountId(), ownerUsername)) {
                return transactionMapper.toDto(tx);
            }
        }

        throw new ResourceNotFoundException("Transaction not found with reference id: " + referenceId);
    }

    @Transactional(readOnly = true)
    public Page<TransactionDto> getTransactionsByAccountId(UUID accountId, String ownerUsername, Pageable pageable) {
        requireAccountOwnership(accountId, ownerUsername);
        return transactionRepository.findByAccountIdOrderByTimestampDescIdDesc(accountId, pageable)
                .map(transactionMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<TransactionDto> getTransactionsByAccountIdAndTimeRange(UUID accountId, LocalDateTime startTime,
                                                                        LocalDateTime endTime, String ownerUsername,
                                                                        Pageable pageable) {
        if (startTime == null || endTime == null || endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("endTime must be equal to or later than startTime");
        }
        requireAccountOwnership(accountId, ownerUsername);
        return transactionRepository.findByAccountIdAndTimeRange(accountId, startTime, endTime, pageable)
                .map(transactionMapper::toDto);
    }

    @Transactional
    public TransactionDto deposit(UUID accountId, BigDecimal amount, String referenceId, String description,
                                  String ownerUsername) {
        requireManualAdjustmentsEnabled();
        Account account = findOwnedAccountForUpdate(accountId, ownerUsername);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }

        if (account.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new IllegalStateException("Account is not active for transactions");
        }

        if (referenceId != null && !referenceId.trim().isEmpty()) {
            Optional<Transaction> existing = transactionRepository.findByAccountIdAndReferenceId(accountId, referenceId.trim());
            if (existing.isPresent()) {
                log.info("Duplicate deposit reference '{}' on account '{}' returning existing transaction", referenceId, accountId);
                return transactionMapper.toDto(existing.get());
            }
        }

        // Update account balance
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);

        // Create transaction record
        Transaction transaction = Transaction.builder()
                .accountId(accountId)
                .amount(amount)
                .type(TransactionType.DEPOSIT)
                .referenceId(referenceId)
                .description(description)
                .timestamp(LocalDateTime.now())
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);
        postBalancedJournal(TransactionType.DEPOSIT, referenceId, description,
                CLEARING_ACCOUNT_ID, accountId, amount);
        transactionMetrics.incrementDepositCount();
        // Send WebSocket notification
        webSocketService.sendTransactionEvent(accountId, savedTransaction);
        return transactionMapper.toDto(savedTransaction);
    }

    @Transactional
    public TransactionDto withdraw(UUID accountId, BigDecimal amount, String referenceId, String description,
                                   String ownerUsername) {
        requireManualAdjustmentsEnabled();
        Account account = findOwnedAccountForUpdate(accountId, ownerUsername);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        }

        if (account.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new IllegalStateException("Account is not active for transactions");
        }

        if (account.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds for withdrawal");
        }

        if (referenceId != null && !referenceId.trim().isEmpty()) {
            Optional<Transaction> existing = transactionRepository.findByAccountIdAndReferenceId(accountId, referenceId.trim());
            if (existing.isPresent()) {
                log.info("Duplicate withdrawal reference '{}' on account '{}' returning existing transaction", referenceId, accountId);
                return transactionMapper.toDto(existing.get());
            }
        }

        // Update account balance
        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);

        // Create transaction record
        Transaction transaction = Transaction.builder()
                .accountId(accountId)
                .amount(amount)
                .type(TransactionType.WITHDRAWAL)
                .referenceId(referenceId)
                .description(description)
                .timestamp(LocalDateTime.now())
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);
        postBalancedJournal(TransactionType.WITHDRAWAL, referenceId, description,
                accountId, CLEARING_ACCOUNT_ID, amount);
        transactionMetrics.incrementWithdrawalCount();
        // Send WebSocket notification
        webSocketService.sendTransactionEvent(accountId, savedTransaction);
        return transactionMapper.toDto(savedTransaction);
    }

    @Transactional
    public TransactionDto transfer(UUID fromAccountId, UUID toAccountId, BigDecimal amount, String referenceId,
                                   String description, String ownerUsername) {
        requireCustomerTransfersEnabled();
        // Validate accounts
        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("Source and destination accounts must be different");
        }

        java.util.Map<UUID, Account> lockedAccounts = accountRepository
                .findAllByIdsForUpdate(java.util.List.of(fromAccountId, toAccountId))
                .stream()
                .collect(java.util.stream.Collectors.toMap(Account::getId, account -> account));
        Account fromAccount = lockedAccounts.get(fromAccountId);
        Account toAccount = lockedAccounts.get(toAccountId);
        if (fromAccount == null || fromAccount.getOwnerUser() == null
                || !ownerUsername.equals(fromAccount.getOwnerUser().getUsername())) {
            throw new ResourceNotFoundException("From account not found with id: " + fromAccountId);
        }
        if (toAccount == null) {
            throw new ResourceNotFoundException("To account not found with id: " + toAccountId);
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }

        if (maxTransferAmount != null && maxTransferAmount.compareTo(BigDecimal.ZERO) > 0
                && amount.compareTo(maxTransferAmount) > 0) {
            throw new IllegalArgumentException("Transfer amount exceeds maximum single transfer limit of " + maxTransferAmount);
        }

        if (dailyTransferLimit != null && dailyTransferLimit.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal recentOutflow = transactionRepository.sumDailyTransferOutAmount(
                    fromAccountId, LocalDateTime.now().minusHours(24));
            BigDecimal currentTotal = recentOutflow != null ? recentOutflow : BigDecimal.ZERO;
            if (currentTotal.add(amount).compareTo(dailyTransferLimit) > 0) {
                throw new IllegalArgumentException("Transfer exceeds daily transfer limit of " + dailyTransferLimit);
            }
        }

        if (fromAccount.getStatus() != Account.AccountStatus.ACTIVE ||
                toAccount.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new IllegalStateException("One or both accounts are not active for transactions");
        }

        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds for transfer");
        }

        if (referenceId != null && !referenceId.trim().isEmpty()) {
            Optional<Transaction> existing = transactionRepository.findByAccountIdAndReferenceId(fromAccountId, referenceId.trim());
            if (existing.isPresent()) {
                log.info("Duplicate transfer reference '{}' from account '{}' returning existing transaction", referenceId, fromAccountId);
                return transactionMapper.toDto(existing.get());
            }
        }

        // Update balances
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        // Create outgoing transaction
        Transaction outgoingTransaction = Transaction.builder()
                .accountId(fromAccountId)
                .amount(amount)
                .type(TransactionType.TRANSFER_OUT)
                .referenceId(referenceId)
                .description(description)
                .timestamp(LocalDateTime.now())
                .build();

        // Create incoming transaction
        Transaction incomingTransaction = Transaction.builder()
                .accountId(toAccountId)
                .amount(amount)
                .type(TransactionType.TRANSFER_IN)
                .referenceId(referenceId)
                .description(description)
                .timestamp(LocalDateTime.now())
                .build();

        Transaction savedOutgoing = transactionRepository.save(outgoingTransaction);
        Transaction savedIncoming = transactionRepository.save(incomingTransaction);
        postBalancedJournal(TransactionType.TRANSFER_OUT, referenceId, description,
                fromAccountId, toAccountId, amount);
        transactionMetrics.incrementTransferCount();

        // Send WebSocket notifications for both accounts
        webSocketService.sendTransactionEvent(fromAccountId, savedOutgoing);
        webSocketService.sendTransactionEvent(toAccountId, savedIncoming);

        return transactionMapper.toDto(savedOutgoing);
    }

    private Account findOwnedAccountForUpdate(UUID accountId, String ownerUsername) {
        if (isCallerAdmin()) {
            return accountRepository.findAllByIdsForUpdate(java.util.List.of(accountId))
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));
        }
        return accountRepository.findOwnedByIdForUpdate(accountId, ownerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));
    }

    private void requireManualAdjustmentsEnabled() {
        if (!manualAdjustmentsEnabled) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Manual ledger adjustments are disabled");
        }
    }

    private void requireCustomerTransfersEnabled() {
        if (!customerTransfersEnabled) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Customer account transfers are disabled");
        }
    }

    private void requireAccountOwnership(UUID accountId, String ownerUsername) {
        if (isCallerAdmin()) {
            if (!accountRepository.existsById(accountId)) {
                throw new ResourceNotFoundException("Transaction not found");
            }
            return;
        }
        if (!accountRepository.existsByIdAndOwnerUser_Username(accountId, ownerUsername)) {
            throw new ResourceNotFoundException("Transaction not found");
        }
    }

    private boolean isCallerAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    private void postBalancedJournal(TransactionType type, String referenceId, String description,
                                     UUID debitAccountId, UUID creditAccountId, BigDecimal amount) {
        LedgerJournal journal = ledgerJournalRepository.save(LedgerJournal.builder()
                .transactionType(type.name())
                .referenceId(referenceId)
                .description(description)
                .build());

        ledgerEntryRepository.saveAll(java.util.List.of(
                LedgerEntry.builder()
                        .journalId(journal.getId())
                        .accountId(debitAccountId)
                        .direction(LedgerEntry.Direction.DEBIT)
                        .amount(amount)
                        .build(),
                LedgerEntry.builder()
                        .journalId(journal.getId())
                        .accountId(creditAccountId)
                        .direction(LedgerEntry.Direction.CREDIT)
                        .amount(amount)
                        .build()));
    }
}
