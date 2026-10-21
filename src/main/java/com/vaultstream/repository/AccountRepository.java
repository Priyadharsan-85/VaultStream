package com.vaultstream.repository;

import com.vaultstream.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

    Optional<Account> findByAccountNumber(String accountNumber);

    Optional<Account> findByIdAndOwnerUser_Username(UUID id, String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id AND a.ownerUser.username = :username")
    Optional<Account> findOwnedByIdForUpdate(@Param("id") UUID id, @Param("username") String username);

    Optional<Account> findByAccountNumberAndOwnerUser_Username(String accountNumber, String username);

    java.util.List<Account> findAllByOwnerUser_UsernameOrderByCreatedAtDesc(String username);

    boolean existsByIdAndOwnerUser_Username(UUID id, String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id IN :accountIds ORDER BY a.id")
    List<Account> findAllByIdsForUpdate(@Param("accountIds") Collection<UUID> accountIds);

    boolean existsByAccountNumber(String accountNumber);
}
