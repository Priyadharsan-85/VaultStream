package com.vaultstream.demo;

import com.vaultstream.model.Account;
import com.vaultstream.model.LedgerEntry;
import com.vaultstream.model.LedgerJournal;
import com.vaultstream.model.Transaction;
import com.vaultstream.repository.AccountRepository;
import com.vaultstream.repository.LedgerEntryRepository;
import com.vaultstream.repository.LedgerJournalRepository;
import com.vaultstream.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Seeds a small, public, synthetic dataset for the Render portfolio demo only. */
@Component
@Profile("render")
@ConditionalOnProperty(name = "vaultstream.demo.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DemoDataInitializer implements ApplicationRunner {

    public static final String SOURCE_ACCOUNT_NUMBER = "VS-DEMO-0001";
    public static final String DESTINATION_ACCOUNT_NUMBER = "VS-DEMO-0002";

    private static final UUID CLEARING_ACCOUNT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final BigDecimal SOURCE_OPENING_BALANCE = new BigDecimal("1250.00");
    private static final BigDecimal DESTINATION_OPENING_BALANCE = new BigDecimal("400.00");
    private static final List<TransferSample> SAMPLE_TRANSFERS = List.of(
            new TransferSample(22, true, new BigDecimal("125.00")),
            new TransferSample(20, false, new BigDecimal("45.00")),
            new TransferSample(18, true, new BigDecimal("80.00")),
            new TransferSample(16, false, new BigDecimal("70.00")),
            new TransferSample(14, true, new BigDecimal("155.00")),
            new TransferSample(12, false, new BigDecimal("30.00")),
            new TransferSample(10, true, new BigDecimal("90.00")),
            new TransferSample(8, false, new BigDecimal("110.00")),
            new TransferSample(6, true, new BigDecimal("200.00")),
            new TransferSample(4, false, new BigDecimal("65.00")),
            new TransferSample(2, true, new BigDecimal("55.00")));

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerJournalRepository ledgerJournalRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        boolean sourceExists = accountRepository.existsByAccountNumber(SOURCE_ACCOUNT_NUMBER);
        boolean destinationExists = accountRepository.existsByAccountNumber(DESTINATION_ACCOUNT_NUMBER);

        if (sourceExists && destinationExists) {
            return;
        }
        if (sourceExists || destinationExists) {
            throw new IllegalStateException("Portfolio demo seed data is incomplete; inspect the demo database before continuing");
        }
        if (!accountRepository.existsById(CLEARING_ACCOUNT_ID)) {
            throw new IllegalStateException("Ledger clearing account is missing; verify database migrations before seeding demo data");
        }

        Account source = createAccount(SOURCE_ACCOUNT_NUMBER, "Demo Source Account", SOURCE_OPENING_BALANCE);
        Account destination = createAccount(DESTINATION_ACCOUNT_NUMBER, "Demo Destination Account", DESTINATION_OPENING_BALANCE);
        accountRepository.saveAll(List.of(source, destination));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime openingTime = now.minusDays(30);
        createOpeningJournal(source, SOURCE_OPENING_BALANCE, "demo-opening-source");
        createOpeningJournal(destination, DESTINATION_OPENING_BALANCE, "demo-opening-destination");

        List<Transaction> transactions = new ArrayList<>(List.of(
                createTransaction(source, SOURCE_OPENING_BALANCE, Transaction.TransactionType.DEPOSIT,
                        "demo-opening-source", "Synthetic opening balance", openingTime),
                createTransaction(destination, DESTINATION_OPENING_BALANCE, Transaction.TransactionType.DEPOSIT,
                        "demo-opening-destination", "Synthetic opening balance", openingTime)));

        for (int index = 0; index < SAMPLE_TRANSFERS.size(); index++) {
            TransferSample sample = SAMPLE_TRANSFERS.get(index);
            String reference = String.format(Locale.ROOT, "demo-transfer-%03d", index + 1);
            LocalDateTime timestamp = now.minusDays(sample.daysAgo());
            Account from = sample.sourceToDestination() ? source : destination;
            Account to = sample.sourceToDestination() ? destination : source;

            LedgerJournal journal = ledgerJournalRepository.save(LedgerJournal.builder()
                    .transactionType("TRANSFER")
                    .referenceId(reference)
                    .description("Synthetic portfolio demo transfer")
                    .build());
            ledgerEntryRepository.saveAll(List.of(
                    createEntry(journal.getId(), from.getId(), LedgerEntry.Direction.DEBIT, sample.amount()),
                    createEntry(journal.getId(), to.getId(), LedgerEntry.Direction.CREDIT, sample.amount())));

            from.setBalance(from.getBalance().subtract(sample.amount()));
            to.setBalance(to.getBalance().add(sample.amount()));
            transactions.add(createTransaction(from, sample.amount(), Transaction.TransactionType.TRANSFER_OUT,
                    reference, "Synthetic demo transfer", timestamp));
            transactions.add(createTransaction(to, sample.amount(), Transaction.TransactionType.TRANSFER_IN,
                    reference, "Synthetic demo transfer", timestamp));
        }

        accountRepository.saveAll(List.of(source, destination));
        transactionRepository.saveAll(transactions);

        log.info("Seeded synthetic portfolio demo accounts and balanced sample transactions");
    }

    private Account createAccount(String accountNumber, String holderName, BigDecimal openingBalance) {
        return Account.builder()
                .accountNumber(accountNumber)
                .accountHolderName(holderName)
                .balance(openingBalance)
                .status(Account.AccountStatus.ACTIVE)
                .build();
    }

    private void createOpeningJournal(Account account, BigDecimal amount, String reference) {
        LedgerJournal journal = ledgerJournalRepository.save(LedgerJournal.builder()
                .transactionType("OPENING_BALANCE")
                .referenceId(reference)
                .description("Synthetic portfolio demo opening balance")
                .build());
        ledgerEntryRepository.saveAll(List.of(
                createEntry(journal.getId(), CLEARING_ACCOUNT_ID, LedgerEntry.Direction.DEBIT, amount),
                createEntry(journal.getId(), account.getId(), LedgerEntry.Direction.CREDIT, amount)));
    }

    private LedgerEntry createEntry(UUID journalId, UUID accountId, LedgerEntry.Direction direction, BigDecimal amount) {
        return LedgerEntry.builder()
                .journalId(journalId)
                .accountId(accountId)
                .direction(direction)
                .amount(amount)
                .build();
    }

    private Transaction createTransaction(Account account, BigDecimal amount, Transaction.TransactionType type,
                                           String reference, String description, LocalDateTime timestamp) {
        return Transaction.builder()
                .accountId(account.getId())
                .amount(amount)
                .type(type)
                .referenceId(reference)
                .description(description)
                .timestamp(timestamp)
                .build();
    }

    private record TransferSample(int daysAgo, boolean sourceToDestination, BigDecimal amount) {
    }
}
