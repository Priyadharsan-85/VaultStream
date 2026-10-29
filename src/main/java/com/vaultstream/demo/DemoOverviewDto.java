package com.vaultstream.demo;

import com.vaultstream.dto.AccountDto;
import com.vaultstream.dto.TransactionDto;

import java.util.List;

public record DemoOverviewDto(String notice, List<AccountDto> accounts, List<TransactionDto> transactions) {
}
