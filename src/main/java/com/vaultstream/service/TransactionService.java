package com.vaultstream.service;

import com.vaultstream.dto.TransactionDto;
import com.vaultstream.exception.ResourceNotFoundException;
import com.vaultstream.mapper.TransactionMapper;
import com.vaultstream.monitoring.TransactionMetrics;
import com.vaultstream.model.Account;
import com.vaultstream.model.Transaction;
import com.vaultstream.model.Transaction.TransactionType;
import com.vaultstream.repository.AccountRepository;
import com.vaultstream.repository.TransactionRepository;
import com.vaultstream.websocket.WebSocketService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final TransactionMapper transactionMapper;
    private final TransactionMetrics transactionMetrics;
    private final WebSocketService webSocketService;

    @Transactional(readOnly = true)
    public TransactionDto getTransactionById(UUID id, String ownerUsername) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));
        requireAccountOwnership(transaction.getAccountId(), ownerUsername);
        return transactionMapper.toDto(transaction);
    }

    @Transactional(readOnly = true)
    public TransactionDto getTransactionByReferenceId(String referenceId, String ownerUsername) {
        Transaction transaction = transactionRepository.findByReferenceId(referenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with reference id: " + referenceId));
        requireAccountOwnership(transaction.getAccountId(), ownerUsername);
        return transactionMapper.toDto(transaction);
    }

    @Transactional(readOnly = true)
    public List<TransactionDto> getTransactionsByAccountId(UUID accountId, String ownerUsername) {
        requireAccountOwnership(accountId, ownerUsername);
        return transactionRepository.findByAccountIdOrderByTimestampDesc(accountId)
                .stream()
                .map(transactionMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TransactionDto> getTransactionsByAccountIdAndTimeRange(UUID accountId, LocalDateTime startTime,
                                                                        LocalDateTime endTime, String ownerUsername) {
        requireAccountOwnership(accountId, ownerUsername);
        return transactionRepository.findByAccountIdAndTimeRange(accountId, startTime, endTime)
                .stream()
                .map(transactionMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public TransactionDto deposit(UUID accountId, BigDecimal amount, String referenceId, String description,
                                  String ownerUsername) {
        Account account = findOwnedAccount(accountId, ownerUsername);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }

        if (account.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new IllegalStateException("Account is not active for transactions");
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
        transactionMetrics.incrementDepositCount();
        // Send WebSocket notification
        webSocketService.sendTransactionEvent(accountId, savedTransaction);
        return transactionMapper.toDto(savedTransaction);
    }

    @Transactional
    public TransactionDto withdraw(UUID accountId, BigDecimal amount, String referenceId, String description,
                                   String ownerUsername) {
        Account account = findOwnedAccount(accountId, ownerUsername);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        }

        if (account.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new IllegalStateException("Account is not active for transactions");
        }

        if (account.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds for withdrawal");
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
        transactionMetrics.incrementWithdrawalCount();
        // Send WebSocket notification
        webSocketService.sendTransactionEvent(accountId, savedTransaction);
        return transactionMapper.toDto(savedTransaction);
    }

    @Transactional
    public TransactionDto transfer(UUID fromAccountId, UUID toAccountId, BigDecimal amount, String referenceId,
                                   String description, String ownerUsername) {
        // Validate accounts
        Account fromAccount = findOwnedAccount(fromAccountId, ownerUsername);
        Account toAccount = accountRepository.findById(toAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("To account not found with id: " + toAccountId));

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }

        if (fromAccount.getStatus() != Account.AccountStatus.ACTIVE ||
                toAccount.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new IllegalStateException("One or both accounts are not active for transactions");
        }

        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds for transfer");
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
        transactionRepository.save(incomingTransaction);
        transactionMetrics.incrementTransferCount();

        // Send WebSocket notifications for both accounts
        webSocketService.sendTransactionEvent(fromAccountId, savedOutgoing);
        webSocketService.sendTransactionEvent(toAccountId, incomingTransaction);

        return transactionMapper.toDto(savedOutgoing);
    }

    private Account findOwnedAccount(UUID accountId, String ownerUsername) {
        return accountRepository.findByIdAndOwnerUser_Username(accountId, ownerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));
    }

    private void requireAccountOwnership(UUID accountId, String ownerUsername) {
        if (!accountRepository.existsByIdAndOwnerUser_Username(accountId, ownerUsername)) {
            throw new ResourceNotFoundException("Transaction not found");
        }
    }
}
