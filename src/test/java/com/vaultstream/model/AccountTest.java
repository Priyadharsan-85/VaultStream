package com.vaultstream.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AccountTest {
    @Test
    void builderStoresAccountFieldsAndOwner() {
        AppUser owner = AppUser.builder().username("alice").build();
        UUID id = UUID.randomUUID();
        Account account = Account.builder().id(id).accountNumber("1234567890").accountHolderName("Alice Doe")
                .balance(new BigDecimal("25.50")).status(Account.AccountStatus.ACTIVE).ownerUser(owner).build();

        assertEquals(id, account.getId());
        assertEquals("1234567890", account.getAccountNumber());
        assertEquals("Alice Doe", account.getAccountHolderName());
        assertEquals(new BigDecimal("25.50"), account.getBalance());
        assertEquals(Account.AccountStatus.ACTIVE, account.getStatus());
        assertSame(owner, account.getOwnerUser());
    }

    @Test
    void prePersistSetsCreatedAndUpdatedTimes() {
        Account account = new Account();
        LocalDateTime before = LocalDateTime.now();

        account.onCreate();

        LocalDateTime after = LocalDateTime.now();
        assertNotNull(account.getCreatedAt());
        assertNotNull(account.getUpdatedAt());
        assertFalse(account.getCreatedAt().isBefore(before));
        assertFalse(account.getUpdatedAt().isAfter(after));
    }

    @Test
    void preUpdateRefreshesUpdatedTime() throws InterruptedException {
        Account account = new Account();
        LocalDateTime oldUpdatedAt = LocalDateTime.now().minusMinutes(1);
        account.setUpdatedAt(oldUpdatedAt);

        account.onUpdate();

        assertTrue(account.getUpdatedAt().isAfter(oldUpdatedAt));
    }

    @Test
    void exposesAllPersistedStatuses() {
        assertEquals(4, Account.AccountStatus.values().length);
        assertNotNull(Account.AccountStatus.valueOf("PENDING_VERIFICATION"));
    }
}
