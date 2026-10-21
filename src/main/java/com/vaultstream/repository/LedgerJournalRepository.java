package com.vaultstream.repository;

import com.vaultstream.model.LedgerJournal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LedgerJournalRepository extends JpaRepository<LedgerJournal, UUID> {
}
