package com.vaultstream.controller;

import com.vaultstream.dto.AccountDto;
import com.vaultstream.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/accounts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAccountController {

    private final AccountService accountService;

    @PutMapping("/{id}/status")
    public ResponseEntity<AccountDto> updateStatus(@PathVariable UUID id,
                                                   @Valid @RequestBody AccountDto.StatusUpdateRequest request) {
        return ResponseEntity.ok(accountService.updateAccountStatus(
                id, com.vaultstream.model.Account.AccountStatus.valueOf(request.getStatus().name())));
    }
}
