package com.vaultstream.service;

import com.vaultstream.dto.AccountDto;
import com.vaultstream.exception.InsufficientFundsException;
import com.vaultstream.exception.ResourceNotFoundException;
import com.vaultstream.mapper.AccountMapper;
import com.vaultstream.monitoring.AccountMetrics;
import com.vaultstream.model.Account;
import com.vaultstream.model.Account.AccountStatus;
import com.vaultstream.repository.AccountRepository;
import com.vaultstream.websocket.WebSocketService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final AccountMetrics accountMetrics;
    private final WebSocketService webSocketService;

    @Transactional(readOnly = true)
    public AccountDto getAccountById(UUID id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + id));
        return accountMapper.toDto(account);
    }

    @Transactional(readOnly = true)
    public AccountDto getAccountByAccountNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with account number: " + accountNumber));
        return accountMapper.toDto(account);
    }

    @Transactional(readOnly = true)
    public List<AccountDto> getAllAccounts() {
        return accountRepository.findAll().stream()
                .map(accountMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public AccountDto createAccount(AccountDto accountDto) {
        if (accountRepository.existsByAccountNumber(accountDto.getAccountNumber())) {
            throw new IllegalArgumentException("Account with number " + accountDto.getAccountNumber() + " already exists");
        }

        Account account = accountMapper.toEntity(accountDto);
        account.setStatus(AccountStatus.PENDING_VERIFICATION);
        if (account.getBalance() == null) {
            account.setBalance(BigDecimal.ZERO);
        }
        Account savedAccount = accountRepository.save(account);
        accountMetrics.incrementAccountCreatedCount();
        return accountMapper.toDto(savedAccount);
    }

    @Transactional
    public AccountDto updateAccount(UUID id, AccountDto accountDto) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + id));

        // Update fields
        if (accountDto.getAccountHolderName() != null) {
            account.setAccountHolderName(accountDto.getAccountHolderName());
        }
        if (accountDto.getStatus() != null) {
            account.setStatus(com.vaultstream.model.Account.AccountStatus.valueOf(accountDto.getStatus().name()));
        }

        Account updatedAccount = accountRepository.save(account);
        accountMetrics.incrementAccountUpdatedCount();
        return accountMapper.toDto(updatedAccount);
    }

    @Transactional
    public void deleteAccount(UUID id) {
        if (!accountRepository.existsById(id)) {
            throw new ResourceNotFoundException("Account not found with id: " + id);
        }
        accountRepository.deleteById(id);
        accountMetrics.incrementAccountDeletedCount();
    }

    @Transactional
    public AccountDto deposit(UUID accountId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Account is not active for transactions");
        }

        account.setBalance(account.getBalance().add(amount));
        Account updatedAccount = accountRepository.save(account);
        // Send WebSocket balance update notification
        webSocketService.sendBalanceUpdate(accountId, updatedAccount.getBalance().doubleValue());
        return accountMapper.toDto(updatedAccount);
    }

    @Transactional
    public AccountDto withdraw(UUID accountId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Account is not active for transactions");
        }

        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds for withdrawal");
        }

        account.setBalance(account.getBalance().subtract(amount));
        Account updatedAccount = accountRepository.save(account);
        // Send WebSocket balance update notification
        webSocketService.sendBalanceUpdate(accountId, updatedAccount.getBalance().doubleValue());
        return accountMapper.toDto(updatedAccount);
    }
}