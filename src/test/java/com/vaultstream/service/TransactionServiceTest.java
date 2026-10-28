package com.vaultstream.service;

import com.vaultstream.dto.TransactionDto;
import com.vaultstream.exception.ResourceNotFoundException;
import com.vaultstream.mapper.TransactionMapper;
import com.vaultstream.model.Account;
import com.vaultstream.model.AppUser;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
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
    void deposit_WhenCallerIsAdmin_FindsAccountWithoutOwnerRestriction() {
        UUID accountId = UUID.randomUUID();
        Account account = Account.builder().id(accountId).balance(new BigDecimal("10.00"))
                .status(Account.AccountStatus.ACTIVE).build();
        Transaction saved = Transaction.builder().id(UUID.randomUUID()).accountId(accountId)
                .amount(new BigDecimal("50.00")).type(Transaction.TransactionType.DEPOSIT).build();
        TransactionDto dto = TransactionDto.builder().id(saved.getId()).build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        try {
            when(accountRepository.findAllByIdsForUpdate(List.of(accountId))).thenReturn(List.of(account));
            when(transactionRepository.save(any(Transaction.class))).thenReturn(saved);
            when(ledgerJournalRepository.save(any(LedgerJournal.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(transactionMapper.toDto(saved)).thenReturn(dto);

            TransactionDto result = transactionService.deposit(accountId, new BigDecimal("50.00"), "ref-adm", "Admin grant", "admin");
            assertSame(dto, result);
            assertEquals(new BigDecimal("60.00"), account.getBalance());
            verify(accountRepository).findAllByIdsForUpdate(List.of(accountId));
            verify(accountRepository, never()).findOwnedByIdForUpdate(any(), any());
        } finally {
            SecurityContextHolder.clearContext();
        }
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

    @Test
    void withdrawUpdatesBalanceAndPostsBalancedJournalEntries() {
        UUID accountId = UUID.randomUUID();
        Account account = Account.builder().id(accountId).balance(new BigDecimal("500.00"))
                .status(Account.AccountStatus.ACTIVE).build();
        Transaction saved = Transaction.builder().id(UUID.randomUUID()).accountId(accountId)
                .amount(new BigDecimal("100.00")).type(Transaction.TransactionType.WITHDRAWAL).build();
        TransactionDto dto = TransactionDto.builder().id(saved.getId()).build();
        when(accountRepository.findOwnedByIdForUpdate(accountId, "alice")).thenReturn(Optional.of(account));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(saved);
        when(ledgerJournalRepository.save(any(LedgerJournal.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionMapper.toDto(saved)).thenReturn(dto);

        assertSame(dto, transactionService.withdraw(accountId, new BigDecimal("100.00"), "ref-w", "cash out", "alice"));

        assertEquals(new BigDecimal("400.00"), account.getBalance());
        verify(accountRepository).save(account);
        verify(ledgerEntryRepository).saveAll(argThat(entries ->
                StreamSupport.stream(entries.spliterator(), false).count() == 2));
        verify(transactionMetrics).incrementWithdrawalCount();
        verify(webSocketService).sendTransactionEvent(accountId, saved);
    }

    @Test
    void withdrawWhenInsufficientFundsThrowsIllegalArgumentException() {
        UUID accountId = UUID.randomUUID();
        Account account = Account.builder().id(accountId).balance(new BigDecimal("50.00"))
                .status(Account.AccountStatus.ACTIVE).build();
        when(accountRepository.findOwnedByIdForUpdate(accountId, "alice")).thenReturn(Optional.of(account));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> transactionService.withdraw(accountId, new BigDecimal("100.00"), "ref-w", "cash out", "alice"));

        assertEquals("Insufficient funds for withdrawal", ex.getMessage());
        assertEquals(new BigDecimal("50.00"), account.getBalance());
        verifyNoInteractions(transactionRepository, ledgerJournalRepository, ledgerEntryRepository, transactionMetrics);
    }

    @Test
    void transfer_SuccessUpdatesBalancesAndPostsJournal() {
        UUID fromId = UUID.randomUUID();
        UUID toId = UUID.randomUUID();
        AppUser alice = AppUser.builder().id(UUID.randomUUID()).username("alice").build();
        AppUser bob = AppUser.builder().id(UUID.randomUUID()).username("bob").build();

        Account fromAccount = Account.builder().id(fromId).balance(new BigDecimal("2000.00"))
                .status(Account.AccountStatus.ACTIVE).ownerUser(alice).build();
        Account toAccount = Account.builder().id(toId).balance(new BigDecimal("500.00"))
                .status(Account.AccountStatus.ACTIVE).ownerUser(bob).build();

        when(accountRepository.findAllByIdsForUpdate(List.of(fromId, toId)))
                .thenReturn(List.of(fromAccount, toAccount));
        when(transactionRepository.sumDailyTransferOutAmount(eq(fromId), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("20000.00"));

        Transaction savedOutgoing = Transaction.builder().id(UUID.randomUUID()).accountId(fromId)
                .amount(new BigDecimal("1000.00")).type(Transaction.TransactionType.TRANSFER_OUT).build();
        TransactionDto dto = TransactionDto.builder().id(savedOutgoing.getId()).build();

        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ledgerJournalRepository.save(any(LedgerJournal.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionMapper.toDto(any(Transaction.class))).thenReturn(dto);

        TransactionDto result = transactionService.transfer(
                fromId, toId, new BigDecimal("1000.00"), "ref-tx-1", "Rent payment", "alice");

        assertSame(dto, result);
        assertEquals(new BigDecimal("1000.00"), fromAccount.getBalance());
        assertEquals(new BigDecimal("1500.00"), toAccount.getBalance());
        verify(accountRepository).save(fromAccount);
        verify(accountRepository).save(toAccount);
        verify(transactionRepository, times(2)).save(any(Transaction.class));
        verify(ledgerJournalRepository).save(any(LedgerJournal.class));
        verify(ledgerEntryRepository).saveAll(argThat(entries ->
                StreamSupport.stream(entries.spliterator(), false).count() == 2));
        verify(transactionMetrics).incrementTransferCount();
        verify(webSocketService).sendTransactionEvent(eq(fromId), any(Transaction.class));
        verify(webSocketService).sendTransactionEvent(eq(toId), any(Transaction.class));
    }

    @Test
    void transfer_WhenSameAccount_ThrowsIllegalArgumentException() {
        UUID accountId = UUID.randomUUID();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> transactionService.transfer(accountId, accountId, new BigDecimal("100.00"), "ref", "desc", "alice"));
        assertEquals("Source and destination accounts must be different", ex.getMessage());
        verifyNoInteractions(accountRepository, transactionRepository, ledgerJournalRepository);
    }

    @Test
    void transfer_WhenExceedsSingleTransferLimit_ThrowsIllegalArgumentException() {
        UUID fromId = UUID.randomUUID();
        UUID toId = UUID.randomUUID();
        AppUser alice = AppUser.builder().id(UUID.randomUUID()).username("alice").build();

        Account fromAccount = Account.builder().id(fromId).balance(new BigDecimal("200000.00"))
                .status(Account.AccountStatus.ACTIVE).ownerUser(alice).build();
        Account toAccount = Account.builder().id(toId).balance(new BigDecimal("500.00"))
                .status(Account.AccountStatus.ACTIVE).build();

        when(accountRepository.findAllByIdsForUpdate(List.of(fromId, toId)))
                .thenReturn(List.of(fromAccount, toAccount));

        BigDecimal excessiveAmount = new BigDecimal("50000.01");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> transactionService.transfer(fromId, toId, excessiveAmount, "ref", "desc", "alice"));

        assertTrue(ex.getMessage().contains("Transfer amount exceeds maximum single transfer limit"));
        assertEquals(new BigDecimal("200000.00"), fromAccount.getBalance());
        verify(accountRepository, never()).save(any());
        verifyNoInteractions(ledgerJournalRepository, ledgerEntryRepository);
    }

    @Test
    void transfer_WhenExceedsDailyTransferLimit_ThrowsIllegalArgumentException() {
        UUID fromId = UUID.randomUUID();
        UUID toId = UUID.randomUUID();
        AppUser alice = AppUser.builder().id(UUID.randomUUID()).username("alice").build();

        Account fromAccount = Account.builder().id(fromId).balance(new BigDecimal("200000.00"))
                .status(Account.AccountStatus.ACTIVE).ownerUser(alice).build();
        Account toAccount = Account.builder().id(toId).balance(new BigDecimal("500.00"))
                .status(Account.AccountStatus.ACTIVE).build();

        when(accountRepository.findAllByIdsForUpdate(List.of(fromId, toId)))
                .thenReturn(List.of(fromAccount, toAccount));
        when(transactionRepository.sumDailyTransferOutAmount(eq(fromId), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("80000.00"));

        BigDecimal amount = new BigDecimal("30000.00"); // 80k + 30k = 110k > 100k
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> transactionService.transfer(fromId, toId, amount, "ref", "desc", "alice"));

        assertTrue(ex.getMessage().contains("Transfer exceeds daily transfer limit"));
        assertEquals(new BigDecimal("200000.00"), fromAccount.getBalance());
        verify(accountRepository, never()).save(any());
        verifyNoInteractions(ledgerJournalRepository, ledgerEntryRepository);
    }

    @Test
    void transfer_WhenInsufficientFunds_ThrowsIllegalArgumentException() {
        UUID fromId = UUID.randomUUID();
        UUID toId = UUID.randomUUID();
        AppUser alice = AppUser.builder().id(UUID.randomUUID()).username("alice").build();

        Account fromAccount = Account.builder().id(fromId).balance(new BigDecimal("100.00"))
                .status(Account.AccountStatus.ACTIVE).ownerUser(alice).build();
        Account toAccount = Account.builder().id(toId).balance(new BigDecimal("500.00"))
                .status(Account.AccountStatus.ACTIVE).build();

        when(accountRepository.findAllByIdsForUpdate(List.of(fromId, toId)))
                .thenReturn(List.of(fromAccount, toAccount));
        when(transactionRepository.sumDailyTransferOutAmount(eq(fromId), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.ZERO);

        BigDecimal amount = new BigDecimal("500.00");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> transactionService.transfer(fromId, toId, amount, "ref", "desc", "alice"));

        assertEquals("Insufficient funds for transfer", ex.getMessage());
        assertEquals(new BigDecimal("100.00"), fromAccount.getBalance());
        verify(accountRepository, never()).save(any());
        verifyNoInteractions(ledgerJournalRepository, ledgerEntryRepository);
    }

    @Test
    void getTransactionByReferenceId_WhenTransferHasTwoTransactions_ReturnsTheCallersTransaction() {
        String refId = "ref-multi-123";
        UUID aliceAccountId = UUID.randomUUID();
        UUID bobAccountId = UUID.randomUUID();

        Transaction aliceTx = Transaction.builder().id(UUID.randomUUID()).accountId(aliceAccountId)
                .amount(new BigDecimal("50.00")).type(Transaction.TransactionType.TRANSFER_OUT)
                .referenceId(refId).build();
        Transaction bobTx = Transaction.builder().id(UUID.randomUUID()).accountId(bobAccountId)
                .amount(new BigDecimal("50.00")).type(Transaction.TransactionType.TRANSFER_IN)
                .referenceId(refId).build();
        TransactionDto aliceDto = TransactionDto.builder().id(aliceTx.getId()).build();

        when(transactionRepository.findAllByReferenceId(refId)).thenReturn(List.of(bobTx, aliceTx));
        when(accountRepository.existsByIdAndOwnerUser_Username(bobAccountId, "alice")).thenReturn(false);
        when(accountRepository.existsByIdAndOwnerUser_Username(aliceAccountId, "alice")).thenReturn(true);
        when(transactionMapper.toDto(aliceTx)).thenReturn(aliceDto);

        TransactionDto result = transactionService.getTransactionByReferenceId(refId, "alice");

        assertSame(aliceDto, result);
        verify(accountRepository).existsByIdAndOwnerUser_Username(bobAccountId, "alice");
        verify(accountRepository).existsByIdAndOwnerUser_Username(aliceAccountId, "alice");
    }

    @Test
    void deposit_WhenDuplicateReferenceId_ReturnsExistingTransactionWithoutMutatingBalance() {
        UUID accountId = UUID.randomUUID();
        String refId = "dup-ref-1";
        Account account = Account.builder().id(accountId).balance(new BigDecimal("1000.00"))
                .status(Account.AccountStatus.ACTIVE).build();
        Transaction existing = Transaction.builder().id(UUID.randomUUID()).accountId(accountId)
                .amount(new BigDecimal("100.00")).type(Transaction.TransactionType.DEPOSIT)
                .referenceId(refId).build();
        TransactionDto dto = TransactionDto.builder().id(existing.getId()).build();

        when(accountRepository.findOwnedByIdForUpdate(accountId, "alice")).thenReturn(Optional.of(account));
        when(transactionRepository.findByAccountIdAndReferenceId(accountId, refId)).thenReturn(Optional.of(existing));
        when(transactionMapper.toDto(existing)).thenReturn(dto);

        TransactionDto result = transactionService.deposit(accountId, new BigDecimal("100.00"), refId, "deposit", "alice");

        assertSame(dto, result);
        assertEquals(new BigDecimal("1000.00"), account.getBalance());
        verify(accountRepository, never()).save(any());
        verifyNoInteractions(ledgerJournalRepository, ledgerEntryRepository, transactionMetrics, webSocketService);
    }

    @Test
    void transfer_WhenDuplicateReferenceId_ReturnsExistingTransactionWithoutMutatingBalances() {
        UUID fromId = UUID.randomUUID();
        UUID toId = UUID.randomUUID();
        String refId = "dup-tx-ref";
        AppUser alice = AppUser.builder().id(UUID.randomUUID()).username("alice").build();
        Account fromAccount = Account.builder().id(fromId).balance(new BigDecimal("2000.00"))
                .status(Account.AccountStatus.ACTIVE).ownerUser(alice).build();
        Account toAccount = Account.builder().id(toId).balance(new BigDecimal("500.00"))
                .status(Account.AccountStatus.ACTIVE).build();
        Transaction existing = Transaction.builder().id(UUID.randomUUID()).accountId(fromId)
                .amount(new BigDecimal("250.00")).type(Transaction.TransactionType.TRANSFER_OUT)
                .referenceId(refId).build();
        TransactionDto dto = TransactionDto.builder().id(existing.getId()).build();

        when(accountRepository.findAllByIdsForUpdate(List.of(fromId, toId))).thenReturn(List.of(fromAccount, toAccount));
        when(transactionRepository.sumDailyTransferOutAmount(eq(fromId), any(LocalDateTime.class))).thenReturn(BigDecimal.ZERO);
        when(transactionRepository.findByAccountIdAndReferenceId(fromId, refId)).thenReturn(Optional.of(existing));
        when(transactionMapper.toDto(existing)).thenReturn(dto);

        TransactionDto result = transactionService.transfer(fromId, toId, new BigDecimal("250.00"), refId, "transfer", "alice");

        assertSame(dto, result);
        assertEquals(new BigDecimal("2000.00"), fromAccount.getBalance());
        assertEquals(new BigDecimal("500.00"), toAccount.getBalance());
        verify(accountRepository, never()).save(any());
        verifyNoInteractions(ledgerJournalRepository, ledgerEntryRepository, transactionMetrics, webSocketService);
    }
}
