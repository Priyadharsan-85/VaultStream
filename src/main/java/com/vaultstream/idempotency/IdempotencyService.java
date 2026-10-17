package com.vaultstream.idempotency;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyRepository idempotencyRepository;

    @Transactional(readOnly = true)
    public Optional<String> getResponseIfExists(String key, String method, String path, String parameters) {
        Optional<IdempotencyKey> existingKey = idempotencyRepository.findByKey(key);
        if (existingKey.isPresent()) {
            IdempotencyKey keyEntity = existingKey.get();
            // Check if it's not expired
            if (keyEntity.getExpiresAt().isAfter(LocalDateTime.now())) {
                // Return cached response
                return Optional.ofNullable(keyEntity.getResponseBody());
            } else {
                // Remove expired key
                idempotencyRepository.delete(keyEntity);
            }
        }
        return Optional.empty();
    }

    @Transactional
    public void saveResponse(String key, String method, String path, String parameters,
                           int responseCode, String responseBody) {
        IdempotencyKey idempotencyKey = IdempotencyKey.builder()
                .key(key)
                .requestMethod(method)
                .requestPath(path)
                .requestParameters(parameters)
                .responseCode(responseCode)
                .responseBody(responseBody)
                .build();
        idempotencyRepository.save(idempotencyKey);
    }
}