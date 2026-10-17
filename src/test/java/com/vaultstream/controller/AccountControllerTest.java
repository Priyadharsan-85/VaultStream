package com.vaultstream.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaultstream.dto.AccountDto;
import com.vaultstream.dto.AccountDto.AccountStatus;
import com.vaultstream.dto.AccountDto.DepositRequest;
import com.vaultstream.dto.AccountDto.WithdrawRequest;
import com.vaultstream.service.AccountService;
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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(AccountController.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccountService accountService;

    @InjectMocks
    private AccountController accountController;

    private UUID testAccountId;
    private AccountDto testAccountDto;
    private List<AccountDto> testAccountList;

    @BeforeEach
    void setUp() {
        testAccountId = UUID.randomUUID();
        testAccountDto = AccountDto.builder()
                .id(testAccountId)
                .accountNumber("TEST123456")
                .accountHolderName("Test User")
                .balance(BigDecimal.valueOf(1000.00))
                .status(AccountStatus.ACTIVE)
                .build();

        testAccountList = Arrays.asList(testAccountDto);
    }

    @Test
    void getAccountById_ShouldReturnAccount_WhenAccountExists() throws Exception {
        // Arrange
        when(accountService.getAccountById(testAccountId)).thenReturn(testAccountDto);

        // Act & Assert
        mockMvc.perform(get("/api/accounts/{id}", testAccountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testAccountId.toString()))
                .andExpect(jsonPath("$.accountNumber").value("TEST123456"))
                .andExpect(jsonPath("$.accountHolderName").value("Test User"))
                .andExpect(jsonPath("$.balance").value(1000.00))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(accountService).getAccountById(testAccountId);
    }

    @Test
    void getAccountById_ShouldReturnNotFound_WhenAccountDoesNotExist() throws Exception {
        // Arrange
        when(accountService.getAccountById(testAccountId)).thenThrow(
                new com.vaultstream.exception.ResourceNotFoundException("Account not found with id: " + testAccountId));

        // Act & Assert
        mockMvc.perform(get("/api/accounts/{id}", testAccountId))
                .andExpect(status().isNotFound());

        verify(accountService).getAccountById(testAccountId);
    }

    @Test
    void getAccountByAccountNumber_ShouldReturnAccount_WhenAccountExists() throws Exception {
        // Arrange
        when(accountService.getAccountByAccountNumber("TEST123456")).thenReturn(testAccountDto);

        // Act & Assert
        mockMvc.perform(get("/api/accounts/number/{accountNumber}", "TEST123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("TEST123456"))
                .andExpect(jsonPath("$.accountHolderName").value("Test User"));

        verify(accountService).getAccountByAccountNumber("TEST123456");
    }

    @Test
    void getAccountByAccountNumber_ShouldReturnNotFound_WhenAccountDoesNotExist() throws Exception {
        // Arrange
        when(accountService.getAccountByAccountNumber("NONEXISTENT")).thenThrow(
                new com.vaultstream.exception.ResourceNotFoundException("Account not found with account number: NONEXISTENT"));

        // Act & Assert
        mockMvc.perform(get("/api/accounts/number/{accountNumber}", "NONEXISTENT"))
                .andExpect(status().isNotFound());

        verify(accountService).getAccountByAccountNumber("NONEXISTENT");
    }

    @Test
    void getAllAccounts_ShouldReturnListOfAccounts() throws Exception {
        // Arrange
        when(accountService.getAllAccounts()).thenReturn(testAccountList);

        // Act & Assert
        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(testAccountId.toString()))
                .andExpect(jsonPath("$[0].accountNumber").value("TEST123456"))
                .andExpect(jsonPath("$[0].accountHolderName").value("Test User"))
                .andExpect(jsonPath("$[0].balance").value(1000.00))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isNotEmpty());

        verify(accountService).getAllAccounts();
    }

    @Test
    void createAccount_ShouldReturnCreatedAccount_WhenValidDtoProvided() throws Exception {
        // Arrange
        AccountDto inputDto = AccountDto.builder()
                .accountNumber("NEW987654")
                .accountHolderName("New User")
                .balance(BigDecimal.valueOf(500.00))
                .build();

        AccountDto createdDto = AccountDto.builder()
                .id(UUID.randomUUID())
                .accountNumber("NEW987654")
                .accountHolderName("New User")
                .balance(BigDecimal.valueOf(500.00))
                .status(AccountStatus.PENDING_VERIFICATION)
                .build();

        when(accountService.createAccount(inputDto)).thenReturn(createdDto);

        // Act & Assert
        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountNumber").value("NEW987654"))
                .andExpect(jsonPath("$.accountHolderName").value("New User"))
                .andExpect(jsonPath("$.balance").value(500.00))
                .andExpect(jsonPath("$.status").value("PENDING_VERIFICATION"));

        verify(accountService).createAccount(inputDto);
    }

    @Test
    void createAccount_ShouldReturnBadRequest_WhenInvalidDtoProvided() throws Exception {
        // Arrange
        AccountDto invalidDto = AccountDto.builder()
                .accountNumber("SHORT") // Too short (min 10)
                .accountHolderName("A") // Too short (min 2)
                .balance(BigDecimal.valueOf(-100.00)) // Negative balance
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());

        verify(accountService, never()).createAccount(any());
    }

    @Test
    void updateAccount_ShouldReturnUpdatedAccount_WhenValidDtoProvided() throws Exception {
        // Arrange
        AccountDto updateDto = AccountDto.builder()
                .accountHolderName("Updated Name")
                .status(AccountStatus.SUSPENDED)
                .build();

        AccountDto updatedDto = AccountDto.builder()
                .id(testAccountId)
                .accountNumber("TEST123456")
                .accountHolderName("Updated Name")
                .balance(BigDecimal.valueOf(1000.00))
                .status(AccountStatus.SUSPENDED)
                .build();

        when(accountService.updateAccount(testAccountId, updateDto)).thenReturn(updatedDto);

        // Act & Assert
        mockMvc.perform(put("/api/accounts/{id}", testAccountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountHolderName").value("Updated Name"))
                .andExpect(jsonPath("$.status").value("SUSPENDED"));

        verify(accountService).updateAccount(testAccountId, updateDto);
    }

    @Test
    void updateAccount_ShouldReturnNotFound_WhenAccountDoesNotExist() throws Exception {
        // Arrange
        AccountDto updateDto = AccountDto.builder()
                .accountHolderName("Updated Name")
                .build();

        when(accountService.updateAccount(testAccountId, updateDto)).thenThrow(
                new com.vaultstream.exception.ResourceNotFoundException("Account not found with id: " + testAccountId));

        // Act & Assert
        mockMvc.perform(put("/api/accounts/{id}", testAccountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isNotFound());

        verify(accountService).updateAccount(testAccountId, updateDto);
    }

    @Test
    void deleteAccount_ShouldReturnNoContent_WhenAccountExists() throws Exception {
        // Arrange
        doNothing().when(accountService).deleteAccount(testAccountId);

        // Act & Assert
        mockMvc.perform(delete("/api/accounts/{id}", testAccountId))
                .andExpect(status().isNoContent());

        verify(accountService).deleteAccount(testAccountId);
    }

    @Test
    void deleteAccount_ShouldReturnNotFound_WhenAccountDoesNotExist() throws Exception {
        // Arrange
        doThrow(new com.vaultstream.exception.ResourceNotFoundException("Account not found with id: " + testAccountId))
                .when(accountService).deleteAccount(testAccountId);

        // Act & Assert
        mockMvc.perform(delete("/api/accounts/{id}", testAccountId))
                .andExpect(status().isNotFound());

        verify(accountService).deleteAccount(testAccountId);
    }

    @Test
    void deposit_ShouldReturnUpdatedAccount_WhenValidAmountProvided() throws Exception {
        // Arrange
        DepositRequest depositRequest = new DepositRequest(BigDecimal.valueOf(250.00));
        AccountDto updatedDto = AccountDto.builder()
                .id(testAccountId)
                .accountNumber("TEST123456")
                .accountHolderName("Test User")
                .balance(BigDecimal.valueOf(1250.00)) // 1000 + 250
                .status(AccountStatus.ACTIVE)
                .build();

        when(accountService.deposit(testAccountId, BigDecimal.valueOf(250.00))).thenReturn(updatedDto);

        // Act & Assert
        mockMvc.perform(post("/api/accounts/{id}/deposit", testAccountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(1250.00))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(accountService).deposit(testAccountId, BigDecimal.valueOf(250.00));
    }

    @Test
    void deposit_ShouldReturnBadRequest_WhenInvalidAmountProvided() throws Exception {
        // Arrange
        DepositRequest invalidRequest = new DepositRequest(BigDecimal.valueOf(-10.00)); // Negative amount

        // Act & Assert
        mockMvc.perform(post("/api/accounts/{id}/deposit", testAccountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(accountService, never()).deposit(any(), any());
    }

    @Test
    void withdraw_ShouldReturnUpdatedAccount_WhenValidAmountProvided() throws Exception {
        // Arrange
        WithdrawRequest withdrawRequest = new WithdrawRequest(BigDecimal.valueOf(250.00));
        AccountDto updatedDto = AccountDto.builder()
                .id(testAccountId)
                .accountNumber("TEST123456")
                .accountHolderName("Test User")
                .balance(BigDecimal.valueOf(750.00)) // 1000 - 250
                .status(AccountStatus.ACTIVE)
                .build();

        when(accountService.withdraw(testAccountId, BigDecimal.valueOf(250.00))).thenReturn(updatedDto);

        // Act & Assert
        mockMvc.perform(post("/api/accounts/{id}/withdraw", testAccountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(withdrawRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(750.00))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(accountService).withdraw(testAccountId, BigDecimal.valueOf(250.00));
    }

    @Test
    void withdraw_ShouldReturnBadRequest_WhenInvalidAmountProvided() throws Exception {
        // Arrange
        WithdrawRequest invalidRequest = new WithdrawRequest(BigDecimal.valueOf(-10.00)); // Negative amount

        // Act & Assert
        mockMvc.perform(post("/api/accounts/{id}/withdraw", testAccountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(accountService, never()).withdraw(any(), any());
    }
}