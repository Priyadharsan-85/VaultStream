package com.vaultstream.mapper;

import com.vaultstream.dto.AccountDto;
import com.vaultstream.dto.AccountDto.AccountStatus;
import com.vaultstream.model.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AccountMapperTest {

    private AccountMapper accountMapper;

    @BeforeEach
    void setUp() {
        accountMapper = Mappers.getMapper(AccountMapper.class);
    }

    @Test
    void toDto_ShouldConvertAccountToAccountDto() {
        // Arrange
        UUID accountId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Account account = Account.builder()
                .id(accountId)
                .accountNumber("TEST123456")
                .accountHolderName("Test User")
                .balance(BigDecimal.valueOf(1000.00))
                .status(Account.AccountStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();

        // Act
        AccountDto dto = accountMapper.toDto(account);

        // Assert
        assertNotNull(dto);
        assertEquals(accountId, dto.getId());
        assertEquals("TEST123456", dto.getAccountNumber());
        assertEquals("Test User", dto.getAccountHolderName());
        assertEquals(BigDecimal.valueOf(1000.00), dto.getBalance());
        assertEquals(AccountStatus.ACTIVE, dto.getStatus());
        assertEquals(now, dto.getCreatedAt());
        assertEquals(now, dto.getUpdatedAt());
    }

    @Test
    void toEntity_ShouldConvertAccountDtoToAccount() {
        // Arrange
        UUID accountId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        AccountDto dto = AccountDto.builder()
                .id(accountId)
                .accountNumber("TEST123456")
                .accountHolderName("Test User")
                .balance(BigDecimal.valueOf(1000.00))
                .status(AccountStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();

        // Act
        Account account = accountMapper.toEntity(dto);

        // Assert
        assertNotNull(account);
        assertEquals(accountId, account.getId());
        assertEquals("TEST123456", account.getAccountNumber());
        assertEquals("Test User", account.getAccountHolderName());
        assertEquals(BigDecimal.valueOf(1000.00), account.getBalance());
        assertEquals(Account.AccountStatus.ACTIVE, account.getStatus());
        assertEquals(now, account.getCreatedAt());
        assertEquals(now, account.getUpdatedAt());
    }

    @Test
    void toDto_ShouldHandleNullValues() {
        // Arrange
        Account account = Account.builder()
                .id(null)
                .accountNumber(null)
                .accountHolderName(null)
                .balance(null)
                .status(null)
                .createdAt(null)
                .updatedAt(null)
                .build();

        // Act
        AccountDto dto = accountMapper.toDto(account);

        // Assert
        assertNotNull(dto);
        assertNull(dto.getId());
        assertNull(dto.getAccountNumber());
        assertNull(dto.getAccountHolderName());
        assertNull(dto.getBalance());
        assertNull(dto.getStatus());
        assertNull(dto.getCreatedAt());
        assertNull(dto.getUpdatedAt());
    }

    @Test
    void toEntity_ShouldHandleNullValues() {
        // Arrange
        AccountDto dto = AccountDto.builder()
                .id(null)
                .accountNumber(null)
                .accountHolderName(null)
                .balance(null)
                .status(null)
                .createdAt(null)
                .updatedAt(null)
                .build();

        // Act
        Account account = accountMapper.toEntity(dto);

        // Assert
        assertNotNull(account);
        assertNull(account.getId());
        assertNull(account.getAccountNumber());
        assertNull(account.getAccountHolderName());
        assertNull(account.getBalance());
        assertNull(account.getStatus());
        assertNull(account.getCreatedAt());
        assertNull(account.getUpdatedAt());
    }
}
