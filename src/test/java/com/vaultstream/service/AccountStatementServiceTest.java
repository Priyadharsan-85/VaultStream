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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountStatementServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Mock
    private TransactionMapper transactionMapper;

    @InjectMocks
    private AccountStatementService statementService;

    private UUID accountId;
    private Account testAccount;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @BeforeEach
    void setUp() {
        accountId = UUID.randomUUID();
        testAccount = Account.builder()
                .id(accountId)
                .accountNumber("ACCT1234567890")
                .accountHolderName("Jane Doe")
                .balance(new BigDecimal("1500.00"))
                .build();
        startTime = LocalDateTime.now().minusDays(30);
        endTime = LocalDateTime.now();
    }

    @Test
    void generateStatement_SuccessWithMultipleTransactions() {
        Transaction txDeposit = Transaction.builder()
                .id(UUID.randomUUID()).accountId(accountId).amount(new BigDecimal("500.00"))
                .type(Transaction.TransactionType.DEPOSIT).timestamp(startTime.plusDays(1)).build();
        Transaction txWithdrawal = Transaction.builder()
                .id(UUID.randomUUID()).accountId(accountId).amount(new BigDecimal("200.00"))
                .type(Transaction.TransactionType.WITHDRAWAL).timestamp(startTime.plusDays(5)).build();
        Transaction txTransferOut = Transaction.builder()
                .id(UUID.randomUUID()).accountId(accountId).amount(new BigDecimal("150.00"))
                .type(Transaction.TransactionType.TRANSFER_OUT).timestamp(startTime.plusDays(10)).build();
        Transaction txTransferIn = Transaction.builder()
                .id(UUID.randomUUID()).accountId(accountId).amount(new BigDecimal("350.00"))
                .type(Transaction.TransactionType.TRANSFER_IN).timestamp(startTime.plusDays(15)).build();

        List<Transaction> transactions = List.of(txDeposit, txWithdrawal, txTransferOut, txTransferIn);

        when(accountRepository.findByIdAndOwnerUser_Username(accountId, "jane")).thenReturn(Optional.of(testAccount));
        when(ledgerEntryRepository.calculateLedgerBalanceBefore(accountId, startTime)).thenReturn(new BigDecimal("1000.00"));
        when(transactionRepository.findAllByAccountIdAndTimeRange(accountId, startTime, endTime)).thenReturn(transactions);
        when(transactionMapper.toDto(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction tx = invocation.getArgument(0);
            return TransactionDto.builder()
                    .id(tx.getId())
                    .amount(tx.getAmount())
                    .type(TransactionDto.TransactionType.valueOf(tx.getType().name()))
                    .build();
        });

        AccountStatementDto statement = statementService.generateStatement(accountId, startTime, endTime, "jane");

        assertNotNull(statement);
        assertEquals(accountId, statement.getAccountId());
        assertEquals("ACCT1234567890", statement.getAccountNumber());
        assertEquals("Jane Doe", statement.getAccountHolderName());
        assertEquals(new BigDecimal("1000.00"), statement.getOpeningBalance());
        assertEquals(new BigDecimal("1500.00"), statement.getClosingBalance());
        assertEquals(new BigDecimal("500.00"), statement.getNetCashFlow());
        assertEquals(new BigDecimal("500.00"), statement.getTotalDeposits());
        assertEquals(new BigDecimal("200.00"), statement.getTotalWithdrawals());
        assertEquals(new BigDecimal("350.00"), statement.getTotalTransfersIn());
        assertEquals(new BigDecimal("150.00"), statement.getTotalTransfersOut());
        assertEquals(4, statement.getTotalTransactions());
        assertEquals(4, statement.getTransactions().size());

        verify(accountRepository).findByIdAndOwnerUser_Username(accountId, "jane");
        verify(ledgerEntryRepository).calculateLedgerBalanceBefore(accountId, startTime);
        verify(transactionRepository).findAllByAccountIdAndTimeRange(accountId, startTime, endTime);
    }

    @Test
    void generateStatement_SuccessWhenEmptyPeriod() {
        when(accountRepository.findByIdAndOwnerUser_Username(accountId, "jane")).thenReturn(Optional.of(testAccount));
        when(ledgerEntryRepository.calculateLedgerBalanceBefore(accountId, startTime)).thenReturn(new BigDecimal("500.00"));
        when(transactionRepository.findAllByAccountIdAndTimeRange(accountId, startTime, endTime)).thenReturn(Collections.emptyList());

        AccountStatementDto statement = statementService.generateStatement(accountId, startTime, endTime, "jane");

        assertNotNull(statement);
        assertEquals(new BigDecimal("500.00"), statement.getOpeningBalance());
        assertEquals(new BigDecimal("500.00"), statement.getClosingBalance());
        assertEquals(BigDecimal.ZERO, statement.getNetCashFlow());
        assertEquals(0, statement.getTotalTransactions());
        assertTrue(statement.getTransactions().isEmpty());
    }

    @Test
    void generateStatement_WhenAdminCalls_AccessesAccountDirectly() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        try {
            when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
            when(ledgerEntryRepository.calculateLedgerBalanceBefore(accountId, startTime)).thenReturn(new BigDecimal("200.00"));
            when(transactionRepository.findAllByAccountIdAndTimeRange(accountId, startTime, endTime)).thenReturn(Collections.emptyList());

            AccountStatementDto statement = statementService.generateStatement(accountId, startTime, endTime, "admin");

            assertNotNull(statement);
            assertEquals(new BigDecimal("200.00"), statement.getOpeningBalance());
            verify(accountRepository).findById(accountId);
            verify(accountRepository, never()).findByIdAndOwnerUser_Username(any(), any());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void generateStatement_RejectsNullDates() {
        assertThrows(IllegalArgumentException.class,
                () -> statementService.generateStatement(accountId, null, endTime, "jane"));
        assertThrows(IllegalArgumentException.class,
                () -> statementService.generateStatement(accountId, startTime, null, "jane"));
    }

    @Test
    void generateStatement_RejectsEndTimeBeforeStartTime() {
        assertThrows(IllegalArgumentException.class,
                () -> statementService.generateStatement(accountId, endTime, startTime, "jane"));
    }

    @Test
    void generateStatement_RejectsPeriodExceedingMaxDays() {
        LocalDateTime tooFarInPast = LocalDateTime.now().minusDays(400);
        assertThrows(IllegalArgumentException.class,
                () -> statementService.generateStatement(accountId, tooFarInPast, LocalDateTime.now(), "jane"));
    }

    @Test
    void generateStatement_ThrowsResourceNotFoundWhenAccountDoesNotExist() {
        when(accountRepository.findByIdAndOwnerUser_Username(accountId, "jane")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> statementService.generateStatement(accountId, startTime, endTime, "jane"));
    }
}
