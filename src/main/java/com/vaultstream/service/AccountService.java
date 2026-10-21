package com.vaultstream.service;

import com.vaultstream.dto.AccountDto;
import com.vaultstream.exception.ResourceNotFoundException;
import com.vaultstream.mapper.AccountMapper;
import com.vaultstream.monitoring.AccountMetrics;
import com.vaultstream.model.Account;
import com.vaultstream.model.Account.AccountStatus;
import com.vaultstream.repository.AccountRepository;
import com.vaultstream.repository.AppUserRepository;
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
    private final AppUserRepository appUserRepository;
    private final AccountMapper accountMapper;
    private final AccountMetrics accountMetrics;

    @Transactional(readOnly = true)
    public AccountDto getAccountById(UUID id, String ownerUsername) {
        Account account = accountRepository.findByIdAndOwnerUser_Username(id, ownerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + id));
        return accountMapper.toDto(account);
    }

    @Transactional(readOnly = true)
    public AccountDto getAccountByAccountNumber(String accountNumber, String ownerUsername) {
        Account account = accountRepository.findByAccountNumberAndOwnerUser_Username(accountNumber, ownerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with account number: " + accountNumber));
        return accountMapper.toDto(account);
    }

    @Transactional(readOnly = true)
    public List<AccountDto> getAllAccounts(String ownerUsername) {
        return accountRepository.findAllByOwnerUser_UsernameOrderByCreatedAtDesc(ownerUsername).stream()
                .map(accountMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public AccountDto createAccount(AccountDto.CreateRequest request, String ownerUsername) {
        if (accountRepository.existsByAccountNumber(request.getAccountNumber())) {
            throw new IllegalArgumentException("Account with number " + request.getAccountNumber() + " already exists");
        }

        Account account = Account.builder()
                .accountNumber(request.getAccountNumber())
                .accountHolderName(request.getAccountHolderName())
                .balance(BigDecimal.ZERO)
                .status(AccountStatus.PENDING_VERIFICATION)
                .ownerUser(appUserRepository.findByUsername(ownerUsername)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found")))
                .build();
        Account savedAccount = accountRepository.save(account);
        accountMetrics.incrementAccountCreatedCount();
        return accountMapper.toDto(savedAccount);
    }

    @Transactional
    public AccountDto updateAccount(UUID id, AccountDto accountDto, String ownerUsername) {
        Account account = accountRepository.findByIdAndOwnerUser_Username(id, ownerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + id));

        // Update fields
        if (accountDto.getAccountHolderName() != null) {
            account.setAccountHolderName(accountDto.getAccountHolderName());
        }
        Account updatedAccount = accountRepository.save(account);
        accountMetrics.incrementAccountUpdatedCount();
        return accountMapper.toDto(updatedAccount);
    }

    @Transactional
    public AccountDto updateAccountStatus(UUID id, AccountStatus status) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + id));
        account.setStatus(status);
        Account updatedAccount = accountRepository.save(account);
        accountMetrics.incrementAccountUpdatedCount();
        return accountMapper.toDto(updatedAccount);
    }

    @Transactional
    public void deleteAccount(UUID id, String ownerUsername) {
        Account account = accountRepository.findByIdAndOwnerUser_Username(id, ownerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + id));
        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalStateException("Account must have a zero balance before it can be closed");
        }
        account.setStatus(AccountStatus.CLOSED);
        accountRepository.save(account);
        accountMetrics.incrementAccountDeletedCount();
    }

}
