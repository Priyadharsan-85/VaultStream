package com.vaultstream.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.time.LocalDateTime;

@Repository
public interface IdempotencyRepository extends JpaRepository<IdempotencyKey, UUID> {

    Optional<IdempotencyKey> findByKey(String key);

    boolean existsByKey(String key);

    @Modifying
    @Query("DELETE FROM IdempotencyKey k WHERE k.expiresAt <= :now")
    int deleteExpired(@Param("now") LocalDateTime now);
}
