package com.vaultstream.controller;

import com.vaultstream.dto.AccountDto;
import com.vaultstream.dto.AccountStatementDto;
import com.vaultstream.service.AccountService;
import com.vaultstream.service.AccountStatementService;
import com.vaultstream.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    private static final String USERNAME = "test-user";

    @Mock
    private AccountService accountService;

    @Mock
    private TransactionService transactionService;

    @Mock
    private AccountStatementService accountStatementService;

    @InjectMocks
    private AccountController accountController;

    private final Principal principal = () -> USERNAME;
    private UUID accountId;
    private AccountDto account;

    @BeforeEach
    void setUp() {
        accountId = UUID.randomUUID();
        account = AccountDto.builder()
                .id(accountId)
                .accountNumber("TEST123456")
                .accountHolderName("Test User")
                .balance(new BigDecimal("1000.00"))
                .status(AccountDto.AccountStatus.ACTIVE)
                .build();
    }

    @Test
    void getAccountByIdPassesAuthenticatedOwner() {
        when(accountService.getAccountById(accountId, USERNAME)).thenReturn(account);

        ResponseEntity<AccountDto> response = accountController.getAccountById(accountId, principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(account, response.getBody());
        verify(accountService).getAccountById(accountId, USERNAME);
    }

    @Test
    void getAccountByAccountNumberPassesAuthenticatedOwner() {
        when(accountService.getAccountByAccountNumber("TEST123456", USERNAME)).thenReturn(account);

        ResponseEntity<AccountDto> response = accountController.getAccountByAccountNumber("TEST123456", principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(account, response.getBody());
        verify(accountService).getAccountByAccountNumber("TEST123456", USERNAME);
    }

    @Test
    void getAllAccountsPassesAuthenticatedOwner() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        Page<AccountDto> accounts = new PageImpl<>(List.of(account), pageable, 1);
        when(accountService.getAllAccounts(USERNAME, pageable)).thenReturn(accounts);

        ResponseEntity<Page<AccountDto>> response = accountController.getAllAccounts(0, 20, principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(accounts, response.getBody());
        verify(accountService).getAllAccounts(USERNAME, pageable);
    }

    @Test
    void getAccountStatement_PassesParametersToStatementService() {
        LocalDateTime start = LocalDateTime.now().minusDays(7);
        LocalDateTime end = LocalDateTime.now();
        AccountStatementDto statement = AccountStatementDto.builder()
                .accountId(accountId)
                .openingBalance(new BigDecimal("100.00"))
                .closingBalance(new BigDecimal("150.00"))
                .build();

        when(accountStatementService.generateStatement(accountId, start, end, USERNAME)).thenReturn(statement);

        ResponseEntity<AccountStatementDto> response = accountController.getAccountStatement(accountId, start, end, principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(statement, response.getBody());
        verify(accountStatementService).generateStatement(accountId, start, end, USERNAME);
    }

    @Test
    void createAccountReturnsCreatedAccountAndPassesAuthenticatedOwner() {
        AccountDto.CreateRequest request = new AccountDto.CreateRequest("TEST123456", "Test User");
        when(accountService.createAccount(request, USERNAME)).thenReturn(account);

        ResponseEntity<AccountDto> response = accountController.createAccount(request, principal);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertSame(account, response.getBody());
        verify(accountService).createAccount(request, USERNAME);
    }

    @Test
    void updateAccountPassesAuthenticatedOwner() {
        when(accountService.updateAccount(accountId, account, USERNAME)).thenReturn(account);

        ResponseEntity<AccountDto> response = accountController.updateAccount(accountId, account, principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(account, response.getBody());
        verify(accountService).updateAccount(accountId, account, USERNAME);
    }

    @Test
    void deleteAccountReturnsNoContentAndPassesAuthenticatedOwner() {
        doNothing().when(accountService).deleteAccount(accountId, USERNAME);

        ResponseEntity<Void> response = accountController.deleteAccount(accountId, principal);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(accountService).deleteAccount(accountId, USERNAME);
    }

    @Test
    void depositUsesTransactionServiceAndReturnsUpdatedAccount() {
        BigDecimal amount = new BigDecimal("250.00");
        AccountDto.DepositRequest request = new AccountDto.DepositRequest(amount);
        when(accountService.getAccountById(accountId, USERNAME)).thenReturn(account);

        ResponseEntity<AccountDto> response = accountController.deposit(accountId, request, principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(account, response.getBody());
        verify(transactionService).deposit(accountId, amount, null, null, USERNAME);
        verify(accountService).getAccountById(accountId, USERNAME);
    }

    @Test
    void withdrawUsesTransactionServiceAndReturnsUpdatedAccount() {
        BigDecimal amount = new BigDecimal("250.00");
        AccountDto.WithdrawRequest request = new AccountDto.WithdrawRequest(amount);
        when(accountService.getAccountById(accountId, USERNAME)).thenReturn(account);

        ResponseEntity<AccountDto> response = accountController.withdraw(accountId, request, principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(account, response.getBody());
        verify(transactionService).withdraw(accountId, amount, null, null, USERNAME);
        verify(accountService).getAccountById(accountId, USERNAME);
    }
}
