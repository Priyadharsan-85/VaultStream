package com.vaultstream.controller;

import com.vaultstream.dto.TransactionDto;
import com.vaultstream.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Validated
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping("/{id}")
    public ResponseEntity<TransactionDto> getTransactionById(@PathVariable UUID id) {
        TransactionDto transactionDto = transactionService.getTransactionById(id);
        return ResponseEntity.ok(transactionDto);
    }

    @GetMapping("/reference/{referenceId}")
    public ResponseEntity<TransactionDto> getTransactionByReferenceId(@PathVariable String referenceId) {
        TransactionDto transactionDto = transactionService.getTransactionByReferenceId(referenceId);
        return ResponseEntity.ok(transactionDto);
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<TransactionDto>> getTransactionsByAccountId(@PathVariable UUID accountId) {
        List<TransactionDto> transactions = transactionService.getTransactionsByAccountId(accountId);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/account/{accountId}/range")
    public ResponseEntity<List<TransactionDto>> getTransactionsByAccountIdAndTimeRange(
            @PathVariable UUID accountId,
            @RequestParam LocalDateTime startTime,
            @RequestParam LocalDateTime endTime) {
        List<TransactionDto> transactions = transactionService.getTransactionsByAccountIdAndTimeRange(accountId, startTime, endTime);
        return ResponseEntity.ok(transactions);
    }

    @PostMapping("/deposit")
    public ResponseEntity<TransactionDto> deposit(@Valid @RequestBody TransactionDto.DepositRequest request) {
        TransactionDto transactionDto = transactionService.deposit(
                request.getAccountId(),
                request.getAmount(),
                request.getReferenceId(),
                request.getDescription());
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionDto);
    }

    @PostMapping("/withdraw")
    public ResponseEntity<TransactionDto> withdraw(@Valid @RequestBody TransactionDto.WithdrawRequest request) {
        TransactionDto transactionDto = transactionService.withdraw(
                request.getAccountId(),
                request.getAmount(),
                request.getReferenceId(),
                request.getDescription());
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionDto);
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransactionDto> transfer(@Valid @RequestBody TransactionDto.TransferRequest request) {
        TransactionDto transactionDto = transactionService.transfer(
                request.getFromAccountId(),
                request.getToAccountId(),
                request.getAmount(),
                request.getReferenceId(),
                request.getDescription());
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionDto);
    }
}