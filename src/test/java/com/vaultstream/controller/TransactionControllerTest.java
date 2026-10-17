package com.vaultstream.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaultstream.dto.TransactionDto;
import com.vaultstream.dto.TransactionDto.DepositRequest;
import com.vaultstream.dto.TransactionDto.WithdrawRequest;
import com.vaultstream.dto.TransactionDto.TransferRequest;
import com.vaultstream.dto.TransactionDto.TransactionType;
import com.vaultstream.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionService transactionService;

    @InjectMocks
    private TransactionController transactionController;

    private UUID testAccountId;
    private UUID testAccountId2;
    private TransactionDto testTransactionDto;
    private List<TransactionDto> testTransactionList;
    private LocalDateTime testTimestamp;

    @BeforeEach
    void setUp() {
        testAccountId = UUID.randomUUID();
        testAccountId2 = UUID.randomUUID();
        testTimestamp = LocalDateTime.now();

        testTransactionDto = TransactionDto.builder()
                .id(UUID.randomUUID())
                .accountId(testAccountId)
                .amount(BigDecimal.valueOf(100.00))
                .type(TransactionType.DEPOSIT)
                .referenceId("REF123")
                .description("Test deposit")
                .timestamp(testTimestamp)
                .build();

        testTransactionList = Arrays.asList(testTransactionDto);
    }

    @Test
    void getTransactionById_ShouldReturnTransaction_WhenTransactionExists() throws Exception {
        // Arrange
        when(transactionService.getTransactionById(testTransactionDto.getId())).thenReturn(testTransactionDto);

        // Act & Assert
        mockMvc.perform(get("/api/transactions/{id}", testTransactionDto.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testTransactionDto.getId().toString()))
                .andExpect(jsonPath("$.accountId").value(testAccountId.toString()))
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.type").value("DEPOSIT"))
                .andExpect(jsonPath("$.referenceId").value("REF123"))
                .andExpect(jsonPath("$.description").value("Test deposit"))
                .andExpect(jsonPath("$.timestamp").value(testTimestamp.toString()));

        verify(transactionService).getTransactionById(testTransactionDto.getId());
    }

    @Test
    void getTransactionById_ShouldReturnNotFound_WhenTransactionDoesNotExist() throws Exception {
        // Arrange
        when(transactionService.getTransactionById(any(UUID.class))).thenThrow(
                new com.vaultstream.exception.ResourceNotFoundException("Transaction not found with id: "));

        // Act & Assert
        mockMvc.perform(get("/api/transactions/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());

        verify(transactionService).getTransactionById(any(UUID.class));
    }

    @Test
    void getTransactionByReferenceId_ShouldReturnTransaction_WhenTransactionExists() throws Exception {
        // Arrange
        when(transactionService.getTransactionByReferenceId("REF123")).thenReturn(testTransactionDto);

        // Act & Assert
        mockMvc.perform(get("/api/transactions/reference/{referenceId}", "REF123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.referenceId").value("REF123"))
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.type").value("DEPOSIT"));

        verify(transactionService).getTransactionByReferenceId("REF123");
    }

    @Test
    void getTransactionByReferenceId_ShouldReturnNotFound_WhenTransactionDoesNotExist() throws Exception {
        // Arrange
        when(transactionService.getTransactionByReferenceId("NONEXISTENT")).thenThrow(
                new com.vaultstream.exception.ResourceNotFoundException("Transaction not found with reference id: NONEXISTENT"));

        // Act & Assert
        mockMvc.perform(get("/api/transactions/reference/{referenceId}", "NONEXISTENT"))
                .andExpect(status().isNotFound());

        verify(transactionService).getTransactionByReferenceId("NONEXISTENT");
    }

    @Test
    void getTransactionsByAccountId_ShouldReturnListOfTransactions() throws Exception {
        // Arrange
        when(transactionService.getTransactionsByAccountId(testAccountId)).thenReturn(testTransactionList);

        // Act & Assert
        mockMvc.perform(get("/api/transactions/account/{accountId}", testAccountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(testTransactionDto.getId().toString()))
                .andExpect(jsonPath("$[0].accountId").value(testAccountId.toString()))
                .andExpect(jsonPath("$[0].amount").value(100.00))
                .andExpect(jsonPath("$[0].type").value("DEPOSIT"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isNotEmpty());

        verify(transactionService).getTransactionsByAccountId(testAccountId);
    }

    @Test
    void getTransactionsByAccountIdAndTimeRange_ShouldReturnListOfTransactions() throws Exception {
        // Arrange
        LocalDateTime startTime = testTimestamp.minusMinutes(10);
        LocalDateTime endTime = testTimestamp.plusMinutes(10);

        when(transactionService.getTransactionsByAccountIdAndTimeRange(testAccountId, startTime, endTime))
                .thenReturn(testTransactionList);

        // Act & Assert
        mockMvc.perform(get("/api/transactions/account/{accountId}/range", testAccountId)
                        .param("startTime", startTime.toString())
                        .param("endTime", endTime.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(testTransactionDto.getId().toString()))
                .andExpect(jsonPath("$[0].accountId").value(testAccountId.toString()))
                .andExpect(jsonPath("$[0].amount").value(100.00))
                .andExpect(jsonPath("$[0].type").value("DEPOSIT"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isNotEmpty());

        verify(transactionService).getTransactionsByAccountIdAndTimeRange(testAccountId, startTime, endTime);
    }

    @Test
    void deposit_ShouldReturnCreatedTransaction_WhenValidRequestProvided() throws Exception {
        // Arrange
        DepositRequest depositRequest = new DepositRequest(
                testAccountId,
                BigDecimal.valueOf(250.00),
                "DEP456",
                "Test deposit"
        );

        TransactionDto createdTransaction = TransactionDto.builder()
                .id(UUID.randomUUID())
                .accountId(testAccountId)
                .amount(BigDecimal.valueOf(250.00))
                .type(TransactionType.DEPOSIT)
                .referenceId("DEP456")
                .description("Test deposit")
                .timestamp(LocalDateTime.now())
                .build();

        when(transactionService.deposit(
                eq(testAccountId),
                eq(BigDecimal.valueOf(250.00)),
                eq("DEP456"),
                eq("Test deposit")))
                .thenReturn(createdTransaction);

        // Act & Assert
        mockMvc.perform(post("/api/transactions/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountId").value(testAccountId.toString()))
                .andExpect(jsonPath("$.amount").value(250.00))
                .andExpect(jsonPath("$.type").value("DEPOSIT"))
                .andExpect(jsonPath("$.referenceId").value("DEP456"))
                .andExpect(jsonPath("$.description").value("Test deposit"));

        verify(transactionService).deposit(
                eq(testAccountId),
                eq(BigDecimal.valueOf(250.00)),
                eq("DEP456"),
                eq("Test deposit"));
    }

    @Test
    void deposit_ShouldReturnBadRequest_WhenInvalidRequestProvided() throws Exception {
        // Arrange
        DepositRequest invalidRequest = new DepositRequest(
                testAccountId,
                BigDecimal.valueOf(-10.00), // Negative amount
                "DEP456",
                "Test deposit"
        );

        // Act & Assert
        mockMvc.perform(post("/api/transactions/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(transactionService, never()).deposit(any(), any(), any(), any());
    }

    @Test
    void withdraw_ShouldReturnCreatedTransaction_WhenValidRequestProvided() throws Exception {
        // Arrange
        WithdrawRequest withdrawRequest = new WithdrawRequest(
                testAccountId,
                BigDecimal.valueOf(250.00),
                "WDL789",
                "Test withdrawal"
        );

        TransactionDto createdTransaction = TransactionDto.builder()
                .id(UUID.randomUUID())
                .accountId(testAccountId)
                .amount(BigDecimal.valueOf(250.00))
                .type(TransactionType.WITHDRAWAL)
                .referenceId("WDL789")
                .description("Test withdrawal")
                .timestamp(LocalDateTime.now())
                .build();

        when(transactionService.withdraw(
                eq(testAccountId),
                eq(BigDecimal.valueOf(250.00)),
                eq("WDL789"),
                eq("Test withdrawal")))
                .thenReturn(createdTransaction);

        // Act & Assert
        mockMvc.perform(post("/api/transactions/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(withdrawRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountId").value(testAccountId.toString()))
                .andExpect(jsonPath("$.amount").value(250.00))
                .andExpect(jsonPath("$.type").value("WITHDRAWAL"))
                .andExpect(jsonPath("$.referenceId").value("WDL789"))
                .andExpect(jsonPath("$.description").value("Test withdrawal"));

        verify(transactionService).withdraw(
                eq(testAccountId),
                eq(BigDecimal.valueOf(250.00)),
                eq("WDL789"),
                eq("Test withdrawal"));
    }

    @Test
    void withdraw_ShouldReturnBadRequest_WhenInvalidRequestProvided() throws Exception {
        // Arrange
        WithdrawRequest invalidRequest = new WithdrawRequest(
                testAccountId,
                BigDecimal.valueOf(-10.00), // Negative amount
                "WDL789",
                "Test withdrawal"
        );

        // Act & Assert
        mockMvc.perform(post("/api/transactions/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(transactionService, never()).withdraw(any(), any(), any(), any());
    }

    @Test
    void transfer_ShouldReturnCreatedTransaction_WhenValidRequestProvided() throws Exception {
        // Arrange
        TransferRequest transferRequest = new TransferRequest(
                testAccountId,
                testAccountId2,
                BigDecimal.valueOf(300.00),
                "TXF456",
                "Test transfer"
        );

        TransactionDto createdTransaction = TransactionDto.builder()
                .id(UUID.randomUUID())
                .accountId(testAccountId)
                .amount(BigDecimal.valueOf(300.00))
                .type(TransactionType.TRANSFER_OUT)
                .referenceId("TXF456")
                .description("Test transfer")
                .timestamp(LocalDateTime.now())
                .build();

        when(transactionService.transfer(
                eq(testAccountId),
                eq(testAccountId2),
                eq(BigDecimal.valueOf(300.00)),
                eq("TXF456"),
                eq("Test transfer")))
                .thenReturn(createdTransaction);

        // Act & Assert
        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountId").value(testAccountId.toString()))
                .andExpect(jsonPath("$.amount").value(300.00))
                .andExpect(jsonPath("$.type").value("TRANSFER_OUT"))
                .andExpect(jsonPath("$.referenceId").value("TXF456"))
                .andExpect(jsonPath("$.description").value("Test transfer"));

        verify(transactionService).transfer(
                eq(testAccountId),
                eq(testAccountId2),
                eq(BigDecimal.valueOf(300.00)),
                eq("TXF456"),
                eq("Test transfer"));
    }

    @Test
    void transfer_ShouldReturnBadRequest_WhenInvalidRequestProvided() throws Exception {
        // Arrange
        TransferRequest invalidRequest = new TransferRequest(
                testAccountId,
                testAccountId2,
                BigDecimal.valueOf(-10.00), // Negative amount
                "TXF456",
                "Test transfer"
        );

        // Act & Assert
        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(transactionService, never()).transfer(any(), any(), any(), any(), any());
    }
}