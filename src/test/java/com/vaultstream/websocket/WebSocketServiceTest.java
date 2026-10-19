package com.vaultstream.websocket;

import com.vaultstream.dto.TransactionDto;
import com.vaultstream.model.Transaction;
import com.vaultstream.model.Transaction.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.*;

class WebSocketServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private WebSocketService webSocketService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void sendTransactionEvent_WithDto_SendsMessage() {
        // Arrange
        UUID accountId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        TransactionDto transactionDto = new TransactionDto();
        transactionDto.setId(transactionId);
        transactionDto.setAccountId(accountId);
        transactionDto.setAmount(new BigDecimal("100.00"));
        transactionDto.setType(TransactionType.DEPOSIT);
        transactionDto.setDescription("Test deposit");
        transactionDto.setTimestamp(LocalDateTime.now());
        transactionDto.setReferenceId("REF123");

        // Act
        webSocketService.sendTransactionEvent(accountId, transactionDto);

        // Assert
        String destination = "/topic/transactions/" + accountId;
        verify(messagingTemplate, times(1)).convertAndSend(eq(destination), any(TransactionEvent.class));
    }

    @Test
    void sendTransactionEvent_WithEntity_SendsMessage() {
        // Arrange
        UUID accountId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = new Transaction();
        transaction.setId(transactionId);
        transaction.setAccountId(accountId);
        transaction.setAmount(new BigDecimal("50.00"));
        transaction.setType(TransactionType.WITHDRAWAL);
        transaction.setDescription("Test withdrawal");
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setReferenceId("REF456");

        // Act
        webSocketService.sendTransactionEvent(accountId, transaction);

        // Assert
        String destination = "/topic/transactions/" + accountId;
        verify(messagingTemplate, times(1)).convertAndSend(eq(destination), any(TransactionEvent.class));
    }

    @Test
    void sendBalanceUpdate_SendsMessage() {
        // Arrange
        UUID accountId = UUID.randomUUID();
        double balance = 1500.75;

        // Act
        webSocketService.sendBalanceUpdate(accountId, balance);

        // Assert
        String destination = "/topic/accounts/" + accountId + "/balance";
        verify(messagingTemplate, times(1)).convertAndSend(eq(destination), eq(balance));
    }
}