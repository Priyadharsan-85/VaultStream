package com.vaultstream.security;

import com.vaultstream.model.Account;
import com.vaultstream.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final AccountRepository accountRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // In a real implementation, you would look up the user by email/username
        // For this example, we'll treat the username as an account number
        Account account = accountRepository.findByAccountNumber(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        return new User(
                account.getAccountNumber(),
                "", // In a real app, you would have a password field
                true,
                true,
                true,
                true,
                Collections.emptyList()
        );
    }
}