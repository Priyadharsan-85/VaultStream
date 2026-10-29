package com.vaultstream.repository;

import com.vaultstream.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Collection;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findByAccountId(UUID accountId);

    Page<Transaction> findByAccountIdOrderByTimestampDescIdDesc(UUID accountId, Pageable pageable);

    @Query(value = "SELECT t FROM Transaction t WHERE t.accountId = :accountId AND t.timestamp >= :startTime AND t.timestamp <= :endTime ORDER BY t.timestamp DESC, t.id DESC",
            countQuery = "SELECT COUNT(t) FROM Transaction t WHERE t.accountId = :accountId AND t.timestamp >= :startTime AND t.timestamp <= :endTime")
    Page<Transaction> findByAccountIdAndTimeRange(
            @Param("accountId") UUID accountId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.accountId = :accountId AND t.timestamp >= :startTime AND t.timestamp <= :endTime ORDER BY t.timestamp ASC, t.id ASC")
    List<Transaction> findAllByAccountIdAndTimeRange(
            @Param("accountId") UUID accountId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.accountId = :accountId AND t.type = com.vaultstream.model.Transaction.TransactionType.TRANSFER_OUT AND t.timestamp >= :since")
    BigDecimal sumDailyTransferOutAmount(@Param("accountId") UUID accountId, @Param("since") LocalDateTime since);

    Optional<Transaction> findByReferenceId(String referenceId);

    List<Transaction> findAllByReferenceId(String referenceId);

    Optional<Transaction> findByAccountIdAndReferenceId(UUID accountId, String referenceId);

    List<Transaction> findAllByAccountIdInOrderByTimestampAscIdAsc(Collection<UUID> accountIds);

    @Query(value = "SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE account_id = :accountId AND type = :type",
            nativeQuery = true)
    BigDecimal sumAmountByAccountIdAndType(@Param("accountId") UUID accountId, @Param("type") String type);
}
