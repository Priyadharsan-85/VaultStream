package com.vaultstream.websocket;

import com.vaultstream.dto.TransactionDto;
import com.vaultstream.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class WebSocketService {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketService.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Send transaction event to subscribers for a specific account
     * @param accountId The account ID to send notifications to
     * @param transactionDto The transaction data to send
     */
    public void sendTransactionEvent(UUID accountId, TransactionDto transactionDto) {
        String destination = "/topic/transactions/" + accountId;
        TransactionEvent event = new TransactionEvent(
                transactionDto.getId(),
                accountId,
                transactionDto.getAmount(),
                transactionDto.getType().name(),
                transactionDto.getDescription(),
                transactionDto.getTimestamp()
        );

        logger.debug("Sending transaction event to {}: {}", destination, event);
        messagingTemplate.convertAndSend(destination, event);
    }

    /**
     * Send transaction event to subscribers for a specific account
     * @param accountId The account ID to send notifications to
     * @param transaction The transaction entity to send
     */
    public void sendTransactionEvent(UUID accountId, Transaction transaction) {
        String destination = "/topic/transactions/" + accountId;
        TransactionEvent event = new TransactionEvent(
                transaction.getId(),
                accountId,
                transaction.getAmount(),
                transaction.getType().name(),
                transaction.getDescription(),
                transaction.getTimestamp()
        );

        logger.debug("Sending transaction event to {}: {}", destination, event);
        messagingTemplate.convertAndSend(destination, event);
    }

    /**
     * Send balance update event to subscribers for a specific account
     * @param accountId The account ID to send notifications to
     * @param balance The new account balance
     */
    public void sendBalanceUpdate(UUID accountId, Double balance) {
        String destination = "/topic/accounts/" + accountId + "/balance";
        logger.debug("Sending balance update to {}: {}", destination, balance);
        messagingTemplate.convertAndSend(destination, balance);
    }
}