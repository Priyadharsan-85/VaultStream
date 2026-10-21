package com.vaultstream.service;

import com.vaultstream.dto.AccountDto;
import com.vaultstream.exception.ResourceNotFoundException;
import com.vaultstream.mapper.AccountMapper;
import com.vaultstream.model.Account;
import com.vaultstream.model.AppUser;
import com.vaultstream.monitoring.AccountMetrics;
import com.vaultstream.repository.AccountRepository;
import com.vaultstream.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {
    @Mock private AccountRepository accountRepository;
    @Mock private AppUserRepository appUserRepository;
    @Mock private AccountMapper accountMapper;
    @Mock private AccountMetrics accountMetrics;
    @InjectMocks private AccountService accountService;

    @Test
    void getAccountByIdScopesLookupToOwner() {
        UUID id = UUID.randomUUID();
        Account account = Account.builder().id(id).build();
        AccountDto dto = AccountDto.builder().id(id).build();
        when(accountRepository.findByIdAndOwnerUser_Username(id, "alice")).thenReturn(Optional.of(account));
        when(accountMapper.toDto(account)).thenReturn(dto);

        assertSame(dto, accountService.getAccountById(id, "alice"));
        verify(accountRepository).findByIdAndOwnerUser_Username(id, "alice");
    }

    @Test
    void getAccountByIdHidesAccountsOwnedBySomeoneElse() {
        UUID id = UUID.randomUUID();
        when(accountRepository.findByIdAndOwnerUser_Username(id, "alice")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> accountService.getAccountById(id, "alice"));
        verifyNoInteractions(accountMapper);
    }

    @Test
    void getAllAccountsReturnsOnlyOwnerAccounts() {
        Account account = Account.builder().id(UUID.randomUUID()).build();
        AccountDto dto = AccountDto.builder().id(account.getId()).build();
        when(accountRepository.findAllByOwnerUser_UsernameOrderByCreatedAtDesc("alice")).thenReturn(List.of(account));
        when(accountMapper.toDto(account)).thenReturn(dto);

        assertEquals(List.of(dto), accountService.getAllAccounts("alice"));
        verify(accountRepository).findAllByOwnerUser_UsernameOrderByCreatedAtDesc("alice");
    }

    @Test
    void createAccountAssignsOwnerAndStartsPendingWithZeroBalance() {
        AppUser owner = AppUser.builder().username("alice").build();
        AccountDto.CreateRequest request = new AccountDto.CreateRequest("1234567890", "Alice");
        when(accountRepository.existsByAccountNumber(request.getAccountNumber())).thenReturn(false);
        when(appUserRepository.findByUsername("alice")).thenReturn(Optional.of(owner));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(accountMapper.toDto(any(Account.class))).thenAnswer(invocation -> {
            Account saved = invocation.getArgument(0);
            return AccountDto.builder().accountNumber(saved.getAccountNumber()).balance(saved.getBalance())
                    .status(AccountDto.AccountStatus.PENDING_VERIFICATION).build();
        });

        AccountDto result = accountService.createAccount(request, "alice");

        assertEquals("1234567890", result.getAccountNumber());
        assertEquals(BigDecimal.ZERO, result.getBalance());
        verify(accountRepository).save(argThat(account -> account.getOwnerUser() == owner
                && account.getStatus() == Account.AccountStatus.PENDING_VERIFICATION
                && account.getBalance().compareTo(BigDecimal.ZERO) == 0));
        verify(accountMetrics).incrementAccountCreatedCount();
    }
}
