package com.vaultstream.mapper;

import com.vaultstream.dto.TransactionDto;
import com.vaultstream.dto.TransactionDto.TransactionType;
import com.vaultstream.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TransactionMapperTest {

    private TransactionMapper transactionMapper;

    @BeforeEach
    void setUp() {
        transactionMapper = Mappers.getMapper(TransactionMapper.class);
    }

    @Test
    void toDto_ShouldConvertTransactionToTransactionDto() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Transaction transaction = Transaction.builder()
                .id(transactionId)
                .accountId(accountId)
                .amount(BigDecimal.valueOf(100.00))
                .type(TransactionType.DEPOSIT)
                .referenceId("REF123")
                .description("Test transaction")
                .timestamp(now)
                .build();

        // Act
        TransactionDto dto = transactionMapper.toDto(transaction);

        // Assert
        assertNotNull(dto);
        assertEquals(transactionId, dto.getId());
        assertEquals(accountId, dto.getAccountId());
        assertEquals(BigDecimal.valueOf(100.00), dto.getAmount());
        assertEquals(TransactionType.DEPOSIT, dto.getType());
        assertEquals("REF123", dto.getReferenceId());
        assertEquals("Test transaction", dto.getDescription());
        assertEquals(now, dto.getTimestamp());
    }

    @Test
    void toEntity_ShouldConvertTransactionDtoToTransaction() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        TransactionDto dto = TransactionDto.builder()
                .id(transactionId)
                .accountId(accountId)
                .amount(BigDecimal.valueOf(100.00))
                .type(TransactionType.WITHDRAWAL)
                .referenceId("REF456")
                .description("Test transaction dto")
                .timestamp(now)
                .build();

        // Act
        Transaction transaction = transactionMapper.toEntity(dto);

        // Assert
        assertNotNull(transaction);
        assertEquals(transactionId, transaction.getId());
        assertEquals(accountId, transaction.getAccountId());
        assertEquals(BigDecimal.valueOf(100.00), transaction.getAmount());
        assertEquals(TransactionType.WITHDRAWAL, transaction.getType());
        assertEquals("REF456", transaction.getReferenceId());
        assertEquals("Test transaction dto", transaction.getDescription());
        assertEquals(now, transaction.getTimestamp());
    }

    @Test
    void toDto_ShouldHandleNullValues() {
        // Arrange
        Transaction transaction = Transaction.builder()
                .id(null)
                .accountId(null)
                .amount(null)
                .type(null)
                .referenceId(null)
                .description(null)
                .timestamp(null)
                .build();

        // Act
        TransactionDto dto = transactionMapper.toDto(transaction);

        // Assert
        assertNotNull(dto);
        assertNull(dto.getId());
        assertNull(dto.getAccountId());
        assertNull(dto.getAmount());
        assertNull(dto.getType());
        assertNull(dto.getReferenceId());
        assertNull(dto.getDescription());
        assertNull(dto.getTimestamp());
    }

    @Test
    void toEntity_ShouldHandleNullValues() {
        // Arrange
        TransactionDto dto = TransactionDto.builder()
                .id(null)
                .accountId(null)
                .amount(null)
                .type(null)
                .referenceId(null)
                .description(null)
                .timestamp(null)
                .build();

        // Act
        Transaction transaction = transactionMapper.toEntity(dto);

        // Assert
        assertNotNull(transaction);
        assertNull(transaction.getId());
        assertNull(transaction.getAccountId());
        assertNull(transaction.getAmount());
        assertNull(transaction.getType());
        assertNull(transaction.getReferenceId());
        assertNull(transaction.getDescription());
        assertNull(transaction.getTimestamp());
    }
}