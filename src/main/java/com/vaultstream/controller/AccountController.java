package com.vaultstream.controller;

import com.vaultstream.dto.AccountDto;
import com.vaultstream.service.AccountService;
import com.vaultstream.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.security.Principal;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Validated
public class AccountController {

    private final AccountService accountService;
    private final TransactionService transactionService;

    @GetMapping("/{id}")
    public ResponseEntity<AccountDto> getAccountById(@PathVariable UUID id, Principal principal) {
        AccountDto accountDto = accountService.getAccountById(id, principal.getName());
        return ResponseEntity.ok(accountDto);
    }

    @GetMapping("/number/{accountNumber}")
    public ResponseEntity<AccountDto> getAccountByAccountNumber(@PathVariable String accountNumber, Principal principal) {
        AccountDto accountDto = accountService.getAccountByAccountNumber(accountNumber, principal.getName());
        return ResponseEntity.ok(accountDto);
    }

    @GetMapping
    public ResponseEntity<List<AccountDto>> getAllAccounts(Principal principal) {
        List<AccountDto> accounts = accountService.getAllAccounts(principal.getName());
        return ResponseEntity.ok(accounts);
    }

    @PostMapping
    public ResponseEntity<AccountDto> createAccount(@Valid @RequestBody AccountDto.CreateRequest request, Principal principal) {
        AccountDto createdAccount = accountService.createAccount(request, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdAccount);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccountDto> updateAccount(@PathVariable UUID id, @Valid @RequestBody AccountDto accountDto, Principal principal) {
        AccountDto updatedAccount = accountService.updateAccount(id, accountDto, principal.getName());
        return ResponseEntity.ok(updatedAccount);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(@PathVariable UUID id, Principal principal) {
        accountService.deleteAccount(id, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/deposit")
    public ResponseEntity<AccountDto> deposit(@PathVariable UUID id,
                                              @Valid @RequestBody AccountDto.DepositRequest request,
                                              Principal principal) {
        transactionService.deposit(id, request.getAmount(), null, null, principal.getName());
        AccountDto accountDto = accountService.getAccountById(id, principal.getName());
        return ResponseEntity.ok(accountDto);
    }

    @PostMapping("/{id}/withdraw")
    public ResponseEntity<AccountDto> withdraw(@PathVariable UUID id,
                                               @Valid @RequestBody AccountDto.WithdrawRequest request,
                                               Principal principal) {
        transactionService.withdraw(id, request.getAmount(), null, null, principal.getName());
        AccountDto accountDto = accountService.getAccountById(id, principal.getName());
        return ResponseEntity.ok(accountDto);
    }
}
