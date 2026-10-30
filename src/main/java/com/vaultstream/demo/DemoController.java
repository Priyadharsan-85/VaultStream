package com.vaultstream.demo;

import com.vaultstream.dto.AccountDto;
import com.vaultstream.dto.TransactionDto;
import com.vaultstream.mapper.AccountMapper;
import com.vaultstream.mapper.TransactionMapper;
import com.vaultstream.model.Account;
import com.vaultstream.repository.AccountRepository;
import com.vaultstream.repository.TransactionRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

/** Public read-only endpoints for the Render demo's synthetic sample records. */
@RestController
@RequestMapping("/api/demo")
@Profile("render")
@ConditionalOnProperty(name = "vaultstream.demo.enabled", havingValue = "true")
@RequiredArgsConstructor
public class DemoController {

    private static final List<String> DEMO_ACCOUNT_NUMBERS = List.of(
            DemoDataInitializer.SOURCE_ACCOUNT_NUMBER,
            DemoDataInitializer.DESTINATION_ACCOUNT_NUMBER);
    private static final String PUBLIC_DATA_NOTICE =
            "Synthetic portfolio data only. This demo does not represent real accounts, payments, or funds.";

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AccountMapper accountMapper;
    private final TransactionMapper transactionMapper;
    private final DemoLiveFeedService demoLiveFeedService;

    @GetMapping("/overview")
    public DemoOverviewDto getOverview() {
        List<Account> accounts = accountRepository.findAllByAccountNumberIn(DEMO_ACCOUNT_NUMBERS);
        if (accounts.size() != DEMO_ACCOUNT_NUMBERS.size()) {
            throw new IllegalStateException("Synthetic demo data has not been initialized");
        }

        List<UUID> accountIds = accounts.stream().map(Account::getId).toList();
        List<AccountDto> accountDtos = accounts.stream().map(accountMapper::toDto).toList();
        List<TransactionDto> transactionDtos = transactionRepository
                .findAllByAccountIdInOrderByTimestampAscIdAsc(accountIds)
                .stream()
                .map(transactionMapper::toDto)
                .toList();

        return new DemoOverviewDto(PUBLIC_DATA_NOTICE, accountDtos, transactionDtos);
    }

    @GetMapping(value = "/live", produces = "text/event-stream")
    public SseEmitter getLiveFeed(HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-transform");
        response.setHeader("X-Accel-Buffering", "no");
        return demoLiveFeedService.openStream();
    }
}
