package com.vaultstream.service;

import com.vaultstream.dto.TransactionDto;
import com.vaultstream.exception.ResourceNotFoundException;
import com.vaultstream.mapper.TransactionMapper;
import com.vaultstream.model.Account;
import com.vaultstream.model.LedgerJournal;
import com.vaultstream.model.Transaction;
import com.vaultstream.monitoring.TransactionMetrics;
import com.vaultstream.repository.AccountRepository;
import com.vaultstream.repository.LedgerEntryRepository;
import com.vaultstream.repository.LedgerJournalRepository;
import com.vaultstream.repository.TransactionRepository;
import com.vaultstream.websocket.WebSocketService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {
    @Mock private TransactionRepository transactionRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private LedgerJournalRepository ledgerJournalRepository;
    @Mock private LedgerEntryRepository ledgerEntryRepository;
    @Mock private TransactionMapper transactionMapper;
    @Mock private TransactionMetrics transactionMetrics;
    @Mock private WebSocketService webSocketService;
    @InjectMocks private TransactionService transactionService;

    @Test
    void getTransactionByIdChecksTheOwningAccountBeforeReturning() {
        UUID transactionId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        Transaction transaction = Transaction.builder().id(transactionId).accountId(accountId).build();
        TransactionDto dto = TransactionDto.builder().id(transactionId).build();
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(accountRepository.existsByIdAndOwnerUser_Username(accountId, "alice")).thenReturn(true);
        when(transactionMapper.toDto(transaction)).thenReturn(dto);

        assertSame(dto, transactionService.getTransactionById(transactionId, "alice"));
        verify(accountRepository).existsByIdAndOwnerUser_Username(accountId, "alice");
    }

    @Test
    void getTransactionByIdDoesNotExposeAnotherOwnersTransaction() {
        UUID transactionId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(
                Transaction.builder().id(transactionId).accountId(accountId).build()));
        when(accountRepository.existsByIdAndOwnerUser_Username(accountId, "alice")).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> transactionService.getTransactionById(transactionId, "alice"));
        verifyNoInteractions(transactionMapper);
    }

    @Test
    void depositUpdatesBalanceAndPostsBalancedJournalEntries() {
        UUID accountId = UUID.randomUUID();
        Account account = Account.builder().id(accountId).balance(new BigDecimal("10.00"))
                .status(Account.AccountStatus.ACTIVE).build();
        Transaction saved = Transaction.builder().id(UUID.randomUUID()).accountId(accountId)
                .amount(new BigDecimal("5.00")).type(Transaction.TransactionType.DEPOSIT).build();
        TransactionDto dto = TransactionDto.builder().id(saved.getId()).build();
        when(accountRepository.findOwnedByIdForUpdate(accountId, "alice")).thenReturn(Optional.of(account));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(saved);
        when(ledgerJournalRepository.save(any(LedgerJournal.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionMapper.toDto(saved)).thenReturn(dto);

        assertSame(dto, transactionService.deposit(accountId, new BigDecimal("5.00"), "ref-1", "funding", "alice"));

        assertEquals(new BigDecimal("15.00"), account.getBalance());
        verify(accountRepository).save(account);
        verify(ledgerEntryRepository).saveAll(argThat(entries ->
                StreamSupport.stream(entries.spliterator(), false).count() == 2));
        verify(transactionMetrics).incrementDepositCount();
        verify(webSocketService).sendTransactionEvent(accountId, saved);
    }

    @Test
    void depositRejectsNonPositiveAmountsWithoutChangingBalance() {
        UUID accountId = UUID.randomUUID();
        Account account = Account.builder().id(accountId).balance(new BigDecimal("10.00"))
                .status(Account.AccountStatus.ACTIVE).build();
        when(accountRepository.findOwnedByIdForUpdate(accountId, "alice")).thenReturn(Optional.of(account));

        assertThrows(IllegalArgumentException.class,
                () -> transactionService.deposit(accountId, BigDecimal.ZERO, "ref-1", "funding", "alice"));

        assertEquals(new BigDecimal("10.00"), account.getBalance());
        verifyNoInteractions(transactionRepository, ledgerJournalRepository, ledgerEntryRepository,
                transactionMetrics, webSocketService);
    }
}
