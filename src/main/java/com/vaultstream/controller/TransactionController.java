package com.vaultstream.controller;

import com.vaultstream.dto.TransactionDto;
import com.vaultstream.service.TransactionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;
import java.security.Principal;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Validated
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping("/{id}")
    public ResponseEntity<TransactionDto> getTransactionById(@PathVariable UUID id, Principal principal) {
        TransactionDto transactionDto = transactionService.getTransactionById(id, principal.getName());
        return ResponseEntity.ok(transactionDto);
    }

    @GetMapping("/reference/{referenceId}")
    public ResponseEntity<TransactionDto> getTransactionByReferenceId(@PathVariable String referenceId, Principal principal) {
        TransactionDto transactionDto = transactionService.getTransactionByReferenceId(referenceId, principal.getName());
        return ResponseEntity.ok(transactionDto);
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<Page<TransactionDto>> getTransactionsByAccountId(
            @PathVariable UUID accountId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            Principal principal) {
        Page<TransactionDto> transactions = transactionService.getTransactionsByAccountId(
                accountId, principal.getName(), transactionPage(page, size));
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/account/{accountId}/range")
    public ResponseEntity<Page<TransactionDto>> getTransactionsByAccountIdAndTimeRange(
            @PathVariable UUID accountId,
            @RequestParam LocalDateTime startTime,
            @RequestParam LocalDateTime endTime,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            Principal principal) {
        Page<TransactionDto> transactions = transactionService.getTransactionsByAccountIdAndTimeRange(
                accountId, startTime, endTime, principal.getName(), transactionPage(page, size));
        return ResponseEntity.ok(transactions);
    }

    private Pageable transactionPage(int page, int size) {
        return PageRequest.of(page, size, Sort.by(Sort.Order.desc("timestamp"), Sort.Order.desc("id")));
    }

    @PostMapping("/deposit")
    public ResponseEntity<TransactionDto> deposit(@Valid @RequestBody TransactionDto.DepositRequest request, Principal principal) {
        TransactionDto transactionDto = transactionService.deposit(
                request.getAccountId(),
                request.getAmount(),
                request.getReferenceId(),
                request.getDescription(),
                principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionDto);
    }

    @PostMapping("/withdraw")
    public ResponseEntity<TransactionDto> withdraw(@Valid @RequestBody TransactionDto.WithdrawRequest request, Principal principal) {
        TransactionDto transactionDto = transactionService.withdraw(
                request.getAccountId(),
                request.getAmount(),
                request.getReferenceId(),
                request.getDescription(),
                principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionDto);
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransactionDto> transfer(@Valid @RequestBody TransactionDto.TransferRequest request, Principal principal) {
        TransactionDto transactionDto = transactionService.transfer(
                request.getFromAccountId(),
                request.getToAccountId(),
                request.getAmount(),
                request.getReferenceId(),
                request.getDescription(),
                principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionDto);
    }
}
