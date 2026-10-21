package com.vaultstream.security;

import com.vaultstream.model.AppUser;
import com.vaultstream.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {
    @Mock private AppUserRepository appUserRepository;
    @InjectMocks private UserDetailsServiceImpl userDetailsService;

    @Test
    void loadsCredentialsAndRoleFromAppUser() {
        when(appUserRepository.findByUsername("alice")).thenReturn(Optional.of(AppUser.builder()
                .username("alice").passwordHash("hashed").role("ROLE_USER").build()));

        var details = userDetailsService.loadUserByUsername("alice");

        assertEquals("alice", details.getUsername());
        assertEquals("hashed", details.getPassword());
        assertTrue(details.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_USER")));
        verify(appUserRepository).findByUsername("alice");
    }

    @Test
    void rejectsUnknownUsernames() {
        when(appUserRepository.findByUsername("missing")).thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("missing"));

        assertEquals("User not found with username: missing", exception.getMessage());
    }
}
