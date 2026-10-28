package com.vaultstream.repository;

import com.vaultstream.model.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM LedgerEntry e WHERE e.direction = :direction")
    BigDecimal sumAmountByDirection(@Param("direction") LedgerEntry.Direction direction);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM LedgerEntry e WHERE e.accountId = :accountId AND e.direction = :direction")
    BigDecimal sumAmountByAccountIdAndDirection(@Param("accountId") UUID accountId, @Param("direction") LedgerEntry.Direction direction);

    @Query("SELECT COALESCE(SUM(CASE WHEN e.direction = com.vaultstream.model.LedgerEntry.Direction.CREDIT THEN e.amount ELSE -e.amount END), 0) " +
           "FROM LedgerEntry e WHERE e.accountId = :accountId")
    BigDecimal calculateLedgerBalanceForAccount(@Param("accountId") UUID accountId);

    @Query("SELECT COALESCE(SUM(CASE WHEN e.direction = com.vaultstream.model.LedgerEntry.Direction.CREDIT THEN e.amount ELSE -e.amount END), 0) " +
           "FROM LedgerEntry e WHERE e.accountId = :accountId AND e.createdAt < :timestamp")
    BigDecimal calculateLedgerBalanceBefore(@Param("accountId") UUID accountId, @Param("timestamp") LocalDateTime timestamp);
}
