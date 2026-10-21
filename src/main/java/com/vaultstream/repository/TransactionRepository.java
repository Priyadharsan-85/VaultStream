package com.vaultstream.repository;

import com.vaultstream.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findByAccountId(UUID accountId);

    List<Transaction> findByAccountIdOrderByTimestampDesc(UUID accountId);

    @Query("SELECT t FROM Transaction t WHERE t.accountId = :accountId AND t.timestamp >= :startTime AND t.timestamp <= :endTime")
    List<Transaction> findByAccountIdAndTimeRange(
            @Param("accountId") UUID accountId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    Optional<Transaction> findByReferenceId(String referenceId);

    @Query(value = "SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE account_id = :accountId AND type = :type",
            nativeQuery = true)
    BigDecimal sumAmountByAccountIdAndType(@Param("accountId") UUID accountId, @Param("type") String type);
}
