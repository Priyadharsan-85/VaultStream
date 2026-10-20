package com.vaultstream.repository;

import com.vaultstream.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

    Optional<Account> findByAccountNumber(String accountNumber);

    Optional<Account> findByIdAndOwnerUser_Username(UUID id, String username);

    Optional<Account> findByAccountNumberAndOwnerUser_Username(String accountNumber, String username);

    java.util.List<Account> findAllByOwnerUser_UsernameOrderByCreatedAtDesc(String username);

    boolean existsByIdAndOwnerUser_Username(UUID id, String username);

    boolean existsByAccountNumber(String accountNumber);
}
