package com.vaultstream.demo;

import com.vaultstream.dto.TransactionDto;
import com.vaultstream.model.Account;
import com.vaultstream.repository.AccountRepository;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** Streams bounded, generated events without writing them to the ledger database. */
@Service
@Profile("render")
@ConditionalOnProperty(name = "vaultstream.demo.enabled", havingValue = "true")
@RequiredArgsConstructor
public class DemoLiveFeedService {

    private static final int MAX_STREAMS = 12;
    private static final long STREAM_TIMEOUT_MILLIS = 120_000;
    private static final long EVENT_INTERVAL_SECONDS = 6;
    private static final List<String> DEMO_ACCOUNT_NUMBERS = List.of(
            DemoDataInitializer.SOURCE_ACCOUNT_NUMBER,
            DemoDataInitializer.DESTINATION_ACCOUNT_NUMBER);

    private final AccountRepository accountRepository;
    private final Semaphore streamSlots = new Semaphore(MAX_STREAMS);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "vaultstream-demo-live-feed");
        thread.setDaemon(true);
        return thread;
    });

    public SseEmitter openStream() {
        if (!streamSlots.tryAcquire()) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "The synthetic live feed is at capacity. Please reconnect in a moment.");
        }

        List<Account> accounts;
        try {
            accounts = accountRepository.findAllByAccountNumberIn(DEMO_ACCOUNT_NUMBERS);
            if (accounts.size() != DEMO_ACCOUNT_NUMBERS.size()) {
                throw new IllegalStateException("Synthetic demo data has not been initialized");
            }
        } catch (RuntimeException exception) {
            streamSlots.release();
            throw exception;
        }

        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MILLIS);
        AtomicReference<ScheduledFuture<?>> scheduledTask = new AtomicReference<>();
        AtomicBoolean released = new AtomicBoolean();
        Runnable cleanup = () -> {
            ScheduledFuture<?> task = scheduledTask.get();
            if (task != null) task.cancel(false);
            if (released.compareAndSet(false, true)) streamSlots.release();
        };

        emitter.onCompletion(cleanup);
        emitter.onTimeout(() -> {
            cleanup.run();
            emitter.complete();
        });
        emitter.onError(error -> cleanup.run());

        try {
            ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(() -> {
                try {
                    DemoLiveEventDto event = createEvent(accounts);
                    emitter.send(SseEmitter.event()
                            .id(event.id().toString())
                            .name("ledger-event")
                            .data(event));
                } catch (IOException | IllegalStateException exception) {
                    cleanup.run();
                    emitter.completeWithError(exception);
                }
            }, 1, EVENT_INTERVAL_SECONDS, TimeUnit.SECONDS);
            scheduledTask.set(task);
            if (released.get()) task.cancel(false);
        } catch (RuntimeException exception) {
            cleanup.run();
            emitter.completeWithError(exception);
            throw exception;
        }

        return emitter;
    }

    DemoLiveEventDto createEvent(List<Account> accounts) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Account first = accounts.get(random.nextInt(accounts.size()));
        Account second = accounts.stream().filter(account -> !account.getId().equals(first.getId())).findFirst().orElseThrow();
        BigDecimal amount = BigDecimal.valueOf(random.nextInt(25, 251)).setScale(2);
        UUID eventId = UUID.randomUUID();
        String reference = "LIVE-" + eventId.toString().substring(0, 8).toUpperCase();
        LocalDateTime timestamp = LocalDateTime.now(Clock.systemUTC());

        return switch (random.nextInt(4)) {
            case 0 -> singleEntry(eventId, "Simulated deposit", amount, first, TransactionDto.TransactionType.DEPOSIT, reference, timestamp);
            case 1 -> singleEntry(eventId, "Simulated withdrawal", amount, first, TransactionDto.TransactionType.WITHDRAWAL, reference, timestamp);
            case 2 -> singleEntry(eventId, "Simulated interest", amount, first, TransactionDto.TransactionType.INTEREST, reference, timestamp);
            default -> transfer(eventId, amount, first, second, reference, timestamp);
        };
    }

    private DemoLiveEventDto singleEntry(UUID eventId, String label, BigDecimal amount, Account account,
                                         TransactionDto.TransactionType type, String reference, LocalDateTime timestamp) {
        String description = label + " in demo ledger";
        return new DemoLiveEventDto(eventId, label, amount, timestamp,
                List.of(transaction(account, amount, type, reference, description, timestamp)));
    }

    private DemoLiveEventDto transfer(UUID eventId, BigDecimal amount, Account source, Account destination,
                                      String reference, LocalDateTime timestamp) {
        return new DemoLiveEventDto(eventId, "Simulated transfer", amount, timestamp, List.of(
                transaction(source, amount, TransactionDto.TransactionType.TRANSFER_OUT, reference,
                        "Simulated transfer to demo account", timestamp),
                transaction(destination, amount, TransactionDto.TransactionType.TRANSFER_IN, reference,
                        "Simulated transfer from demo account", timestamp)));
    }

    private TransactionDto transaction(Account account, BigDecimal amount, TransactionDto.TransactionType type,
                                       String reference, String description, LocalDateTime timestamp) {
        return TransactionDto.builder()
                .id(UUID.randomUUID())
                .accountId(account.getId())
                .amount(amount)
                .type(type)
                .referenceId(reference)
                .description(description)
                .timestamp(timestamp)
                .build();
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdownNow();
    }
}
