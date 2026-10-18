package com.vaultstream.repository;

import com.vaultstream.model.Account;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(org.springframework.test.context.junit.jupiter.SpringExtension.class)
@DataJpaTest
@Import(AccountRepository.class)
class AccountRepositoryTest {

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void repository_ShouldNotBeNull() {
        assertNotNull(accountRepository);
    }

    @Test
    void findByAccountNumber_ShouldReturnEmptyOptional_WhenNoAccountExists() {
        // Act
        Optional<Account> result = accountRepository.findByAccountNumber("NONEXISTENT");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void existsByAccountNumber_ShouldReturnFalse_WhenNoAccountExists() {
        // Act
        boolean exists = accountRepository.existsByAccountNumber("NONEXISTENT");

        // Assert
        assertFalse(exists);
    }

    @Test
    void saveAndFindById_ShouldWorkCorrectly() {
        // Arrange
        Account account = Account.builder()
                .accountNumber("TEST123456")
                .accountHolderName("Test User")
                .build();

        // Act
        Account savedAccount = accountRepository.save(account);
        Optional<Account> foundAccount = accountRepository.findById(savedAccount.getId());

        // Assert
        assertNotNull(savedAccount.getId());
        assertTrue(foundAccount.isPresent());
        assertEquals("TEST123456", foundAccount.get().getAccountNumber());
        assertEquals("Test User", foundAccount.get().getAccountHolderName());
    }

    @Test
    void existsByAccountNumber_ShouldReturnTrue_WhenAccountExists() {
        // Arrange
        Account account = Account.builder()
                .accountNumber("EXISTENT123")
                .accountHolderName("Test User")
                .build();
        accountRepository.save(account);

        // Act
        boolean exists = accountRepository.existsByAccountNumber("EXISTENT123");

        // Assert
        assertTrue(exists);
    }

    @Test
    void findByAccountNumber_ShouldReturnAccount_WhenAccountExists() {
        // Arrange
        Account account = Account.builder()
                .accountNumber("FOUND456")
                .accountHolderName("Test User")
                .build();
        accountRepository.save(account);

        // Act
        Optional<Account> result = accountRepository.findByAccountNumber("FOUND456");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("FOUND456", result.get().getAccountNumber());
        assertEquals("Test User", result.get().getAccountHolderName());
    }
}