package com.vaultstream.service;

import com.vaultstream.dto.AccountDto;
import com.vaultstream.exception.InsufficientFundsException;
import com.vaultstream.exception.ResourceNotFoundException;
import com.vaultstream.model.Account;
import com.vaultstream.model.Account.AccountStatus;
import com.vaultstream.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SpringBootTest
@ActiveProfiles("test")
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private com.vaultstream.mapper.AccountMapper accountMapper;

    @Mock
    private com.vaultstream.monitoring.AccountMetrics accountMetrics;

    @InjectMocks
    private AccountService accountService;

    private UUID testAccountId;
    private Account testAccount;
    private AccountDto testAccountDto;

    @BeforeEach
    void setUp() {
        testAccountId = UUID.randomUUID();
        testAccount = Account.builder()
                .id(testAccountId)
                .accountNumber("TEST123456")
                .accountHolderName("Test User")
                .balance(BigDecimal.valueOf(1000.00))
                .status(AccountStatus.ACTIVE)
                .build();

        testAccountDto = AccountDto.builder()
                .id(testAccountId)
                .accountNumber("TEST123456")
                .accountHolderName("Test User")
                .balance(BigDecimal.valueOf(1000.00))
                .status(AccountStatus.ACTIVE)
                .build();
    }

    // Retrieval Tests
    @Test
    void getAccountById_ShouldReturnAccount_WhenAccountExists() {
        // Arrange
        when(accountRepository.findById(testAccountId)).thenReturn(Optional.of(testAccount));
        when(accountMapper.toDto(testAccount)).thenReturn(testAccountDto);

        // Act
        AccountDto result = accountService.getAccountById(testAccountId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testAccountId);
        assertThat(result.getAccountNumber()).isEqualTo("TEST123456");
        verify(accountRepository).findById(testAccountId);
        verify(accountMapper).toDto(testAccount);
    }

    @Test
    void getAccountById_ShouldThrowResourceNotFoundException_WhenAccountDoesNotExist() {
        // Arrange
        when(accountRepository.findById(testAccountId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> accountService.getAccountById(testAccountId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Account not found with id: " + testAccountId);
        verify(accountRepository).findById(testAccountId);
        verify(accountMapper, never()).toDto(any());
    }

    @Test
    void getAccountByAccountNumber_ShouldReturnAccount_WhenAccountExists() {
        // Arrange
        when(accountRepository.findByAccountNumber("TEST123456")).thenReturn(Optional.of(testAccount));
        when(accountMapper.toDto(testAccount)).thenReturn(testAccountDto);

        // Act
        AccountDto result = accountService.getAccountByAccountNumber("TEST123456");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getAccountNumber()).isEqualTo("TEST123456");
        verify(accountRepository).findByAccountNumber("TEST123456");
        verify(accountMapper).toDto(testAccount);
    }

    @Test
    void getAccountByAccountNumber_ShouldThrowResourceNotFoundException_WhenAccountDoesNotExist() {
        // Arrange
        when(accountRepository.findByAccountNumber("NONEXISTENT")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> accountService.getAccountByAccountNumber("NONEXISTENT"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Account not found with account number: NONEXISTENT");
        verify(accountRepository).findByAccountNumber("NONEXISTENT");
        verify(accountMapper, never()).toDto(any());
    }

    @Test
    void getAllAccounts_ShouldReturnListOfAccounts() {
        // Arrange
        when(accountRepository.findAll()).thenReturn(List.of(testAccount));
        when(accountMapper.toDto(testAccount)).thenReturn(testAccountDto);

        // Act
        List<AccountDto> result = accountService.getAllAccounts();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testAccountDto);
        verify(accountRepository).findAll();
        verify(accountMapper).toDto(testAccount);
    }

    // Creation Tests
    @Test
    void createAccount_ShouldCreateAccount_WhenValidDtoProvided() {
        // Arrange
        AccountDto inputDto = AccountDto.builder()
                .accountNumber("NEW987654")
                .accountHolderName("New User")
                .balance(BigDecimal.valueOf(500.00))
                .build();

        Account newAccount = Account.builder()
                .accountNumber("NEW987654")
                .accountHolderName("New User")
                .balance(BigDecimal.valueOf(500.00))
                .status(AccountStatus.PENDING_VERIFICATION)
                .build();

        Account savedAccount = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber("NEW987654")
                .accountHolderName("New User")
                .balance(BigDecimal.valueOf(500.00))
                .status(AccountStatus.PENDING_VERIFICATION)
                .build();

        AccountDto savedDto = AccountDto.builder()
                .id(savedAccount.getId())
                .accountNumber("NEW987654")
                .accountHolderName("New User")
                .balance(BigDecimal.valueOf(500.00))
                .status(AccountStatus.PENDING_VERIFICATION)
                .build();

        when(accountRepository.existsByAccountNumber("NEW987654")).thenReturn(false);
        when(accountMapper.toEntity(inputDto)).thenReturn(newAccount);
        when(accountRepository.save(newAccount)).thenReturn(savedAccount);
        when(accountMapper.toDto(savedAccount)).thenReturn(savedDto);

        // Act
        AccountDto result = accountService.createAccount(inputDto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getAccountNumber()).isEqualTo("NEW987654");
        assertThat(result.getStatus()).isEqualTo(AccountStatus.PENDING_VERIFICATION);
        assertThat(result.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(500.00));
        verify(accountRepository).existsByAccountNumber("NEW987654");
        verify(accountMapper).toEntity(inputDto);
        verify(accountRepository).save(newAccount);
        verify(accountMetrics).incrementAccountCreatedCount();
    }

    @Test
    void createAccount_ShouldThrowIllegalArgumentException_WhenAccountNumberAlreadyExists() {
        // Arrange
        AccountDto inputDto = AccountDto.builder()
                .accountNumber("EXISTING123")
                .accountHolderName("Test User")
                .balance(BigDecimal.valueOf(500.00))
                .build();

        when(accountRepository.existsByAccountNumber("EXISTING123")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> accountService.createAccount(inputDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Account with number EXISTING123 already exists");
        verify(accountRepository).existsByAccountNumber("EXISTING123");
        verify(accountMapper, never()).toEntity(any());
        verify(accountRepository, never()).save(any());
    }

    // Update Tests
    @Test
    void updateAccount_ShouldUpdateAccount_WhenValidDtoProvided() {
        // Arrange
        AccountDto updateDto = AccountDto.builder()
                .accountHolderName("Updated Name")
                .status(AccountStatus.SUSPENDED)
                .build();

        Account updatedAccount = Account.builder()
                .id(testAccountId)
                .accountNumber("TEST123456")
                .accountHolderName("Updated Name")
                .balance(BigDecimal.valueOf(1000.00))
                .status(AccountStatus.SUSPENDED)
                .build();

        AccountDto updatedDto = AccountDto.builder()
                .id(testAccountId)
                .accountNumber("TEST123456")
                .accountHolderName("Updated Name")
                .balance(BigDecimal.valueOf(1000.00))
                .status(AccountStatus.SUSPENDED)
                .build();

        when(accountRepository.findById(testAccountId)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(updatedAccount);
        when(accountMapper.toDto(updatedAccount)).thenReturn(updatedDto);

        // Act
        AccountDto result = accountService.updateAccount(testAccountId, updateDto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getAccountHolderName()).isEqualTo("Updated Name");
        assertThat(result.getStatus()).isEqualTo(AccountStatus.SUSPENDED);
        verify(accountRepository).findById(testAccountId);
        verify(accountRepository).save(any(Account.class));
        verify(accountMetrics).incrementAccountUpdatedCount();
    }

    @Test
    void updateAccount_ShouldThrowResourceNotFoundException_WhenAccountDoesNotExist() {
        // Arrange
        when(accountRepository.findById(testAccountId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> accountService.updateAccount(testAccountId, AccountDto.builder().build()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Account not found with id: " + testAccountId);
        verify(accountRepository).findById(testAccountId);
        verify(accountRepository, never()).save(any());
    }

    // Delete Tests
    @Test
    void deleteAccount_ShouldDeleteAccount_WhenAccountExists() {
        // Arrange
        when(accountRepository.existsById(testAccountId)).thenReturn(true);

        // Act
        accountService.deleteAccount(testAccountId);

        // Assert
        verify(accountRepository).existsById(testAccountId);
        verify(accountRepository).deleteById(testAccountId);
        verify(accountMetrics).incrementAccountDeletedCount();
    }

    @Test
    void deleteAccount_ShouldThrowResourceNotFoundException_WhenAccountDoesNotExist() {
        // Arrange
        when(accountRepository.existsById(testAccountId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> accountService.deleteAccount(testAccountId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Account not found with id: " + testAccountId);
        verify(accountRepository).existsById(testAccountId);
        verify(accountRepository, never()).deleteById(any());
    }

    // Deposit Tests
    @Test
    void deposit_ShouldIncreaseBalance_WhenValidAmountProvided() {
        // Arrange
        BigDecimal depositAmount = BigDecimal.valueOf(250.00);
        Account updatedAccount = Account.builder()
                .id(testAccountId)
                .accountNumber("TEST123456")
                .accountHolderName("Test User")
                .balance(BigDecimal.valueOf(1250.00)) // 1000 + 250
                .status(AccountStatus.ACTIVE)
                .build();

        AccountDto updatedDto = AccountDto.builder()
                .id(testAccountId)
                .accountNumber("TEST123456")
                .accountHolderName("Test User")
                .balance(BigDecimal.valueOf(1250.00))
                .status(AccountStatus.ACTIVE)
                .build();

        when(accountRepository.findById(testAccountId)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(updatedAccount);
        when(accountMapper.toDto(updatedAccount)).thenReturn(updatedDto);

        // Act
        AccountDto result = accountService.deposit(testAccountId, depositAmount);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(1250.00));
        verify(accountRepository).findById(testAccountId);
        verify(accountRepository).save(any(Account.class));
        verify(accountMetrics).incrementAccountCreatedCount(); // Note: This seems to be a bug in the original code - it increments created count instead of deposit count
    }

    @Test
    void deposit_ShouldThrowIllegalArgumentException_WhenAmountIsNullOrZeroOrNegative() {
        // Arrange & Act & Assert
        assertThatThrownBy(() -> accountService.deposit(testAccountId, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Deposit amount must be positive");

        assertThatThrownBy(() -> accountService.deposit(testAccountId, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Deposit amount must be positive");

        assertThatThrownBy(() -> accountService.deposit(testAccountId, BigDecimal.valueOf(-10.00)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Deposit amount must be positive");

        verify(accountRepository, never()).findById(any());
    }

    @Test
    void deposit_ShouldThrowIllegalStateException_WhenAccountIsNotActive() {
        // Arrange
        Account inactiveAccount = Account.builder()
                .id(testAccountId)
                .accountNumber("TEST123456")
                .accountHolderName("Test User")
                .balance(BigDecimal.valueOf(1000.00))
                .status(AccountStatus.SUSPENDED)
                .build();

        when(accountRepository.findById(testAccountId)).thenReturn(Optional.of(inactiveAccount));

        // Act & Assert
        assertThatThrownBy(() -> accountService.deposit(testAccountId, BigDecimal.valueOf(100.00)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Account is not active for transactions");
        verify(accountRepository).findById(testAccountId);
        verify(accountRepository, never()).save(any());
    }

    // Withdrawal Tests
    @Test
    void withdraw_ShouldDecreaseBalance_WhenValidAmountProvided() {
        // Arrange
        BigDecimal withdrawalAmount = BigDecimal.valueOf(250.00);
        Account updatedAccount = Account.builder()
                .id(testAccountId)
                .accountNumber("TEST123456")
                .accountHolderName("Test User")
                .balance(BigDecimal.valueOf(750.00)) // 1000 - 250
                .status(AccountStatus.ACTIVE)
                .build();

        AccountDto updatedDto = AccountDto.builder()
                .id(testAccountId)
                .accountNumber("TEST123456")
                .accountHolderName("Test User")
                .balance(BigDecimal.valueOf(750.00))
                .status(AccountStatus.ACTIVE)
                .build();

        when(accountRepository.findById(testAccountId)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(updatedAccount);
        when(accountMapper.toDto(updatedAccount)).thenReturn(updatedDto);

        // Act
        AccountDto result = accountService.withdraw(testAccountId, withdrawalAmount);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(750.00));
        verify(accountRepository).findById(testAccountId);
        verify(accountRepository).save(any(Account.class));
        // Note: Original code has a bug here too - it increments account created count instead of something appropriate
    }

    @Test
    void withdraw_ShouldThrowIllegalArgumentException_WhenAmountIsNullOrZeroOrNegative() {
        // Arrange & Act & Assert
        assertThatThrownBy(() -> accountService.withdraw(testAccountId, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Withdrawal amount must be positive");

        assertThatThrownBy(() -> accountService.withdraw(testAccountId, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Withdrawal amount must be positive");

        assertThatThrownBy(() -> accountService.withdraw(testAccountId, BigDecimal.valueOf(-10.00)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Withdrawal amount must be positive");

        verify(accountRepository, never()).findById(any());
    }

    @Test
    void withdraw_ShouldThrowIllegalStateException_WhenAccountIsNotActive() {
        // Arrange
        Account inactiveAccount = Account.builder()
                .id(testAccountId)
                .accountNumber("TEST123456")
                .accountHolderName("Test User")
                .balance(BigDecimal.valueOf(1000.00))
                .status(AccountStatus.CLOSED)
                .build();

        when(accountRepository.findById(testAccountId)).thenReturn(Optional.of(inactiveAccount));

        // Act & Assert
        assertThatThrownBy(() -> accountService.withdraw(testAccountId, BigDecimal.valueOf(100.00)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Account is not active for transactions");
        verify(accountRepository).findById(testAccountId);
        verify(accountRepository, never()).save(any());
    }

    @Test
    void withdraw_ShouldThrowInsufficientFundsException_WhenInsufficientFunds() {
        // Arrange
        when(accountRepository.findById(testAccountId)).thenReturn(Optional.of(testAccount)); // Balance: 1000

        // Act & Assert
        assertThatThrownBy(() -> accountService.withdraw(testAccountId, BigDecimal.valueOf(1500.00))) // Try to withdraw 1500
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("Insufficient funds for withdrawal");
        verify(accountRepository).findById(testAccountId);
        verify(accountRepository, never()).save(any());
    }
}