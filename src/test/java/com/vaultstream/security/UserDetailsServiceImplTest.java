package com.vaultstream.security;

import com.vaultstream.model.Account;
import com.vaultstream.model.Account.AccountStatus;
import com.vaultstream.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    private UUID testAccountId;
    private Account testAccount;

    @BeforeEach
    void setUp() {
        testAccountId = UUID.randomUUID();
        testAccount = Account.builder()
                .id(testAccountId)
                .accountNumber("TEST123456")
                .accountHolderName("Test User")
                .balance(1000.00)
                .status(AccountStatus.ACTIVE)
                .build();
    }

    @Test
    void loadUserByUsername_ShouldReturnUserDetails_WhenAccountExists() {
        // Arrange
        when(accountRepository.findByAccountNumber("TEST123456")).thenReturn(Optional.of(testAccount));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername("TEST123456");

        // Assert
        assertNotNull(userDetails);
        assertEquals("TEST123456", userDetails.getUsername());
        assertTrue(userDetails.isEnabled());
        assertTrue(userDetails.isAccountNonExpired());
        assertTrue(userDetails.isCredentialsNonExpired());
        assertTrue(userDetails.isAccountNonLocked());
        assertEquals(0, userDetails.getAuthorities().size()); // No authorities in this implementation
        verify(accountRepository).findByAccountNumber("TEST123456");
    }

    @Test
    void loadUserByUsername_ShouldThrowUsernameNotFoundException_WhenAccountDoesNotExist() {
        // Arrange
        when(accountRepository.findByAccountNumber("NONEXISTENT")).thenReturn(Optional.empty());

        // Act & Assert
        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("NONEXISTENT"));

        assertTrue(exception.getMessage().contains("User not found with username: NONEXISTENT"));
        verify(accountRepository).findByAccountNumber("NONEXISTENT");
    }

    @Test
    void loadUserByUsername_ShouldThrowUsernameNotFoundException_WhenAccountIsNull() {
        // Arrange
        when(accountRepository.findByAccountNumber("NULLACCOUNT")).thenReturn(Optional.empty());

        // Act & Assert
        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("NULLACCOUNT"));

        assertTrue(exception.getMessage().contains("User not found with username: NULLACCOUNT"));
        verify(accountRepository).findByAccountNumber("NULLACCOUNT");
    }

    @Test
    void loadUserByUsername_ShouldHandleDifferentAccountStatuses() {
        // Arrange & Act & Assert
        for (AccountStatus status : AccountStatus.values()) {
            Account accountWithStatus = Account.builder()
                    .id(UUID.randomUUID())
                    .accountNumber("ACCOUNT-" + status)
                    .accountHolderName("Test User")
                    .balance(1000.00)
                    .status(status)
                    .build();

            when(accountRepository.findByAccountNumber("ACCOUNT-" + status)).thenReturn(Optional.of(accountWithStatus));

            // Act
            UserDetails userDetails = userDetailsService.loadUserByUsername("ACCOUNT-" + status);

            // Assert - All account statuses should be treated as valid users (no account disabling logic)
            assertNotNull(userDetails);
            assertEquals("ACCOUNT-" + status, userDetails.getUsername());
            verify(accountRepository).findByAccountNumber("ACCOUNT-" + status);
        }
    }
}