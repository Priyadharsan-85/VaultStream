package com.vaultstream.demo;

import com.vaultstream.dto.TransactionDto;
import com.vaultstream.model.Account;
import com.vaultstream.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class DemoLiveFeedServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Test
    void createEventProducesSyntheticEntriesForDemoAccountsWithoutRepositoryWrites() {
        DemoLiveFeedService service = new DemoLiveFeedService(accountRepository);
        Account source = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber(DemoDataInitializer.SOURCE_ACCOUNT_NUMBER)
                .build();
        Account destination = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber(DemoDataInitializer.DESTINATION_ACCOUNT_NUMBER)
                .build();
        Set<UUID> demoAccountIds = Set.of(source.getId(), destination.getId());

        try {
            for (int index = 0; index < 40; index++) {
                DemoLiveEventDto event = service.createEvent(List.of(source, destination));

                assertNotNull(event.id());
                assertNotNull(event.timestamp());
                assertTrue(event.amount().compareTo(BigDecimal.ZERO) > 0);
                assertTrue(event.transactions().size() == 1 || event.transactions().size() == 2);
                assertTrue(event.transactions().stream().allMatch(transaction -> demoAccountIds.contains(transaction.getAccountId())));
                assertTrue(event.transactions().stream().allMatch(transaction -> event.amount().compareTo(transaction.getAmount()) == 0));

                if (event.label().equals("Simulated transfer")) {
                    assertEquals(2, event.transactions().size());
                    assertEquals(Set.of(TransactionDto.TransactionType.TRANSFER_IN, TransactionDto.TransactionType.TRANSFER_OUT),
                            Set.of(event.transactions().get(0).getType(), event.transactions().get(1).getType()));
                } else {
                    assertEquals(1, event.transactions().size());
                    assertTrue(Set.of(TransactionDto.TransactionType.DEPOSIT, TransactionDto.TransactionType.WITHDRAWAL,
                            TransactionDto.TransactionType.INTEREST).contains(event.transactions().get(0).getType()));
                }
            }
        } finally {
            service.shutdown();
        }

        verifyNoInteractions(accountRepository);
    }
}
