package com.vaultstream.idempotency;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private static final int PENDING_TIMEOUT_MINUTES = 2;

    private final IdempotencyRepository idempotencyRepository;

    @Transactional
    public Reservation reserve(String key, String method, String path, String fingerprint) {
        Optional<IdempotencyKey> existing = idempotencyRepository.findByKey(key);
        if (existing.isPresent()) {
            IdempotencyKey entity = existing.get();
            boolean expired = !entity.getExpiresAt().isAfter(LocalDateTime.now());
            boolean abandoned = entity.getResponseCode() == null
                    && entity.getCreatedAt().plusMinutes(PENDING_TIMEOUT_MINUTES).isBefore(LocalDateTime.now());
            if (expired || abandoned) {
                idempotencyRepository.delete(entity);
                idempotencyRepository.flush();
            } else {
                return toReservation(entity, method, path, fingerprint);
            }
        }

        IdempotencyKey reservation = IdempotencyKey.builder()
                .key(key)
                .requestMethod(method)
                .requestPath(path)
                .requestParameters(fingerprint)
                .build();
        idempotencyRepository.saveAndFlush(reservation);
        return new Reservation(State.RESERVED, null, null, null);
    }

    @Transactional(readOnly = true)
    public Reservation lookup(String key, String method, String path, String fingerprint) {
        return idempotencyRepository.findByKey(key)
                .map(entity -> toReservation(entity, method, path, fingerprint))
                .orElseGet(() -> new Reservation(State.RESERVED, null, null, null));
    }

    @Transactional
    public void complete(String key, int responseCode, String contentType, String responseBody) {
        IdempotencyKey entity = idempotencyRepository.findByKey(key)
                .orElseThrow(() -> new IllegalStateException("Idempotency reservation disappeared"));
        entity.setResponseCode(responseCode);
        entity.setResponseContentType(contentType);
        entity.setResponseBody(responseBody);
        idempotencyRepository.save(entity);
    }

    @Transactional
    public void remove(String key) {
        idempotencyRepository.findByKey(key).ifPresent(idempotencyRepository::delete);
    }

    private Reservation toReservation(IdempotencyKey entity, String method, String path, String fingerprint) {
        if (!entity.getRequestMethod().equals(method)
                || !entity.getRequestPath().equals(path)
                || !entity.getRequestParameters().equals(fingerprint)) {
            return new Reservation(State.CONFLICT, null, null, null);
        }
        if (entity.getResponseCode() == null) {
            return new Reservation(State.IN_PROGRESS, null, null, null);
        }
        return new Reservation(State.REPLAY, entity.getResponseCode(), entity.getResponseContentType(),
                entity.getResponseBody() == null ? "" : entity.getResponseBody());
    }

    public enum State {
        RESERVED,
        REPLAY,
        CONFLICT,
        IN_PROGRESS
    }

    public record Reservation(State state, Integer responseCode, String contentType, String responseBody) { }
}
