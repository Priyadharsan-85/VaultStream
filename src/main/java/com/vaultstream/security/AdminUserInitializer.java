package com.vaultstream.security;

import com.vaultstream.model.AppUser;
import com.vaultstream.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminUserInitializer implements ApplicationRunner {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${vaultstream.security.admin-username:}")
    private String adminUsername;

    @Value("${vaultstream.security.admin-password:}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        if ((adminUsername == null || adminUsername.isBlank())
                && (adminPassword == null || adminPassword.isBlank())) {
            return;
        }
        if (adminUsername == null || adminUsername.isBlank()
                || adminPassword == null || adminPassword.length() < 12 || adminPassword.length() > 72) {
            throw new IllegalStateException("Configure both admin credentials; the password must be 12-72 characters");
        }

        appUserRepository.findByUsername(adminUsername).ifPresentOrElse(existing -> {
            if (!"ROLE_ADMIN".equals(existing.getRole())) {
                throw new IllegalStateException("Configured admin username already belongs to a non-admin user");
            }
        }, () -> appUserRepository.save(AppUser.builder()
                .username(adminUsername)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .role("ROLE_ADMIN")
                .build()));
    }
}
