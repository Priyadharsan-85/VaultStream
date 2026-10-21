package com.vaultstream.controller;

import com.vaultstream.dto.TransactionDto;
import com.vaultstream.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    private static final String USERNAME = "test-user";

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private TransactionController transactionController;

    private final Principal principal = () -> USERNAME;
    private UUID accountId;
    private TransactionDto transaction;

    @BeforeEach
    void setUp() {
        accountId = UUID.randomUUID();
        transaction = TransactionDto.builder()
                .id(UUID.randomUUID())
                .accountId(accountId)
                .amount(new BigDecimal("100.00"))
                .type(TransactionDto.TransactionType.DEPOSIT)
                .referenceId("REF123")
                .description("Test deposit")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Test
    void getTransactionByIdPassesAuthenticatedOwner() {
        when(transactionService.getTransactionById(transaction.getId(), USERNAME)).thenReturn(transaction);

        ResponseEntity<TransactionDto> response = transactionController.getTransactionById(transaction.getId(), principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(transaction, response.getBody());
        verify(transactionService).getTransactionById(transaction.getId(), USERNAME);
    }

    @Test
    void getTransactionByReferenceIdPassesAuthenticatedOwner() {
        when(transactionService.getTransactionByReferenceId("REF123", USERNAME)).thenReturn(transaction);

        ResponseEntity<TransactionDto> response = transactionController.getTransactionByReferenceId("REF123", principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(transaction, response.getBody());
        verify(transactionService).getTransactionByReferenceId("REF123", USERNAME);
    }

    @Test
    void getTransactionsByAccountIdPassesAuthenticatedOwner() {
        List<TransactionDto> transactions = List.of(transaction);
        when(transactionService.getTransactionsByAccountId(accountId, USERNAME)).thenReturn(transactions);

        ResponseEntity<List<TransactionDto>> response = transactionController.getTransactionsByAccountId(accountId, principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(transactions, response.getBody());
        verify(transactionService).getTransactionsByAccountId(accountId, USERNAME);
    }

    @Test
    void getTransactionsByAccountIdAndTimeRangePassesOwnerAndRange() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = start.plusHours(1);
        List<TransactionDto> transactions = List.of(transaction);
        when(transactionService.getTransactionsByAccountIdAndTimeRange(accountId, start, end, USERNAME))
                .thenReturn(transactions);

        ResponseEntity<List<TransactionDto>> response = transactionController
                .getTransactionsByAccountIdAndTimeRange(accountId, start, end, principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(transactions, response.getBody());
        verify(transactionService).getTransactionsByAccountIdAndTimeRange(accountId, start, end, USERNAME);
    }

    @Test
    void depositReturnsCreatedTransactionAndPassesOwner() {
        TransactionDto.DepositRequest request = new TransactionDto.DepositRequest(
                accountId, new BigDecimal("250.00"), "DEP456", "Initial funding");
        when(transactionService.deposit(accountId, request.getAmount(), request.getReferenceId(),
                request.getDescription(), USERNAME)).thenReturn(transaction);

        ResponseEntity<TransactionDto> response = transactionController.deposit(request, principal);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertSame(transaction, response.getBody());
        verify(transactionService).deposit(accountId, request.getAmount(), "DEP456", "Initial funding", USERNAME);
    }

    @Test
    void withdrawReturnsCreatedTransactionAndPassesOwner() {
        TransactionDto.WithdrawRequest request = new TransactionDto.WithdrawRequest(
                accountId, new BigDecimal("50.00"), "WDL789", "Cash withdrawal");
        when(transactionService.withdraw(accountId, request.getAmount(), request.getReferenceId(),
                request.getDescription(), USERNAME)).thenReturn(transaction);

        ResponseEntity<TransactionDto> response = transactionController.withdraw(request, principal);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertSame(transaction, response.getBody());
        verify(transactionService).withdraw(accountId, request.getAmount(), "WDL789", "Cash withdrawal", USERNAME);
    }

    @Test
    void transferReturnsCreatedTransactionAndPassesOwner() {
        UUID destinationAccountId = UUID.randomUUID();
        TransactionDto.TransferRequest request = new TransactionDto.TransferRequest(
                accountId, destinationAccountId, new BigDecimal("75.00"), "TRF999", "Transfer");
        when(transactionService.transfer(accountId, destinationAccountId, request.getAmount(),
                request.getReferenceId(), request.getDescription(), USERNAME)).thenReturn(transaction);

        ResponseEntity<TransactionDto> response = transactionController.transfer(request, principal);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertSame(transaction, response.getBody());
        verify(transactionService).transfer(accountId, destinationAccountId, request.getAmount(),
                "TRF999", "Transfer", USERNAME);
    }
}
