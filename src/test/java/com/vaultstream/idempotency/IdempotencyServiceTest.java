package com.vaultstream.idempotency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    private static final String KEY = "test-key-123";
    private static final String METHOD = "POST";
    private static final String PATH = "/api/test";
    private static final String FINGERPRINT = "request-fingerprint";

    @Mock
    private IdempotencyRepository idempotencyRepository;

    @InjectMocks
    private IdempotencyService idempotencyService;

    private IdempotencyKey existing;

    @BeforeEach
    void setUp() {
        existing = IdempotencyKey.builder()
                .key(KEY)
                .requestMethod(METHOD)
                .requestPath(PATH)
                .requestParameters(FINGERPRINT)
                .responseCode(201)
                .responseContentType("application/json")
                .responseBody("{\"result\":\"success\"}")
                .createdAt(LocalDateTime.now().minusMinutes(1))
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();
    }

    @Test
    void reservesKeyWhenNoExistingRecord() {
        when(idempotencyRepository.findByKey(KEY)).thenReturn(Optional.empty());

        IdempotencyService.Reservation reservation = idempotencyService.reserve(KEY, METHOD, PATH, FINGERPRINT);

        assertEquals(IdempotencyService.State.RESERVED, reservation.state());
        verify(idempotencyRepository).saveAndFlush(argThat(key ->
                KEY.equals(key.getKey()) && METHOD.equals(key.getRequestMethod())
                        && PATH.equals(key.getRequestPath()) && FINGERPRINT.equals(key.getRequestParameters())));
    }

    @Test
    void returnsReplayForMatchingCompletedRequest() {
        when(idempotencyRepository.findByKey(KEY)).thenReturn(Optional.of(existing));

        IdempotencyService.Reservation reservation = idempotencyService.reserve(KEY, METHOD, PATH, FINGERPRINT);

        assertEquals(IdempotencyService.State.REPLAY, reservation.state());
        assertEquals(201, reservation.responseCode());
        assertEquals("application/json", reservation.contentType());
        assertEquals("{\"result\":\"success\"}", reservation.responseBody());
    }

    @Test
    void returnsConflictWhenRequestFingerprintDiffers() {
        when(idempotencyRepository.findByKey(KEY)).thenReturn(Optional.of(existing));

        IdempotencyService.Reservation reservation = idempotencyService.reserve(KEY, METHOD, PATH, "different");

        assertEquals(IdempotencyService.State.CONFLICT, reservation.state());
    }

    @Test
    void returnsInProgressWhenMatchingKeyHasNoResponse() {
        existing.setResponseCode(null);
        existing.setResponseBody(null);
        when(idempotencyRepository.findByKey(KEY)).thenReturn(Optional.of(existing));

        IdempotencyService.Reservation reservation = idempotencyService.reserve(KEY, METHOD, PATH, FINGERPRINT);

        assertEquals(IdempotencyService.State.IN_PROGRESS, reservation.state());
    }

    @Test
    void replacesExpiredReservation() {
        existing.setExpiresAt(LocalDateTime.now().minusSeconds(1));
        when(idempotencyRepository.findByKey(KEY)).thenReturn(Optional.of(existing));

        IdempotencyService.Reservation reservation = idempotencyService.reserve(KEY, METHOD, PATH, FINGERPRINT);

        assertEquals(IdempotencyService.State.RESERVED, reservation.state());
        verify(idempotencyRepository).delete(existing);
        verify(idempotencyRepository).flush();
        verify(idempotencyRepository).saveAndFlush(argThat(key -> KEY.equals(key.getKey())));
    }

    @Test
    void completeStoresResponseFields() {
        when(idempotencyRepository.findByKey(KEY)).thenReturn(Optional.of(existing));

        idempotencyService.complete(KEY, 202, "text/plain", "accepted");

        verify(idempotencyRepository).save(argThat(key -> key == existing
                && key.getResponseCode() == 202
                && "text/plain".equals(key.getResponseContentType())
                && "accepted".equals(key.getResponseBody())));
    }

    @Test
    void lookupReturnsReservedWhenConcurrentRecordIsNotVisible() {
        when(idempotencyRepository.findByKey(KEY)).thenReturn(Optional.empty());

        assertEquals(IdempotencyService.State.RESERVED,
                idempotencyService.lookup(KEY, METHOD, PATH, FINGERPRINT).state());
    }

    @Test
    void removeDeletesExistingKey() {
        when(idempotencyRepository.findByKey(KEY)).thenReturn(Optional.of(existing));

        idempotencyService.remove(KEY);

        verify(idempotencyRepository).delete(existing);
    }
}
