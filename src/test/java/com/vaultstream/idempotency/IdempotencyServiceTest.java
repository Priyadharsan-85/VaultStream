package com.vaultstream.idempotency;

import com.vaultstream.idempotency.IdempotencyKey;
import com.vaultstream.idempotency.IdempotencyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private IdempotencyRepository idempotencyRepository;

    @InjectMocks
    private IdempotencyService idempotencyService;

    private String testKey;
    private String testMethod;
    private String testPath;
    private String testParameters;
    private int testResponseCode;
    private String testResponseBody;
    private IdempotencyKey testIdempotencyKey;

    @BeforeEach
    void setUp() {
        testKey = "test-key-123";
        testMethod = "POST";
        testPath = "/api/test";
        testParameters = "param1=value1";
        testResponseCode = 200;
        testResponseBody = "{\"result\": \"success\"}";

        testIdempotencyKey = IdempotencyKey.builder()
                .key(testKey)
                .requestMethod(testMethod)
                .requestPath(testPath)
                .requestParameters(testParameters)
                .responseCode(testResponseCode)
                .responseBody(testResponseBody)
                .build();
    }

    @Test
    void getResponseIfExists_ShouldReturnResponse_WhenValidUnexpiredKeyExists() {
        // Arrange
        when(idempotencyRepository.findByKey(testKey)).thenReturn(Optional.of(testIdempotencyKey));

        // Act
        Optional<String> result = idempotencyService.getResponseIfExists(testKey, testMethod, testPath, testParameters);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testResponseBody, result.get());
        verify(idempotencyRepository).findByKey(testKey);
    }

    @Test
    void getResponseIfExists_ShouldReturnEmpty_WhenKeyDoesNotExist() {
        // Arrange
        when(idempotencyRepository.findByKey(testKey)).thenReturn(Optional.empty());

        // Act
        Optional<String> result = idempotencyService.getResponseIfExists(testKey, testMethod, testPath, testParameters);

        // Assert
        assertFalse(result.isPresent());
        verify(idempotencyRepository).findByKey(testKey);
    }

    @Test
    void getResponseIfExists_ShouldReturnEmpty_WhenKeyIsExpired() {
        // Arrange
        IdempotencyKey expiredKey = IdempotencyKey.builder()
                .key(testKey)
                .requestMethod(testMethod)
                .requestPath(testPath)
                .requestParameters(testParameters)
                .responseCode(testResponseCode)
                .responseBody(testResponseBody)
                .expiresAt(LocalDateTime.now().minusHours(1)) // Expired 1 hour ago
                .build();

        when(idempotencyRepository.findByKey(testKey)).thenReturn(Optional.of(expiredKey));

        // Act
        Optional<String> result = idempotencyService.getResponseIfExists(testKey, testMethod, testPath, testParameters);

        // Assert
        assertFalse(result.isPresent());
        verify(idempotencyRepository).findByKey(testKey);
        verify(idempotencyRepository).delete(expiredKey);
    }

    @Test
    void saveResponse_ShouldSaveIdempotencyKey_WhenCalled() {
        // Arrange
        IdempotencyKey keyToSave = IdempotencyKey.builder()
                .key(testKey)
                .requestMethod(testMethod)
                .requestPath(testPath)
                .requestParameters(testParameters)
                .responseCode(testResponseCode)
                .responseBody(testResponseBody)
                .build();

        // Act
        idempotencyService.saveResponse(testKey, testMethod, testPath, testParameters, testResponseCode, testResponseBody);

        // Assert
        verify(idempotencyRepository).save(argThat(savedKey ->
                savedKey.getKey().equals(testKey) &&
                savedKey.getRequestMethod().equals(testMethod) &&
                savedKey.getRequestPath().equals(testPath) &&
                savedKey.getRequestParameters().equals(testParameters) &&
                savedKey.getResponseCode() == testResponseCode &&
                savedKey.getResponseBody().equals(testResponseBody)
        ));
    }
}