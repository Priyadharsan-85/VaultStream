package com.vaultstream.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void handleResourceNotFoundException_ShouldReturnNotFoundResponse() {
        // Arrange
        ResourceNotFoundException ex = new ResourceNotFoundException("Account not found with id: 123");
        WebRequest request = mock(WebRequest.class);

        // Act
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> responseEntity =
                globalExceptionHandler.handleResourceNotFoundException(ex);

        // Assert
        assertNotNull(responseEntity);
        assertEquals(404, responseEntity.getStatusCodeValue());
        GlobalExceptionHandler.ErrorResponse errorResponse = responseEntity.getBody();
        assertNotNull(errorResponse);
        assertEquals(404, errorResponse.getStatus());
        assertEquals("NOT_FOUND", errorResponse.getError());
        assertEquals("Account not found with id: 123", errorResponse.getMessage());
        assertNotNull(errorResponse.getTimestamp());
    }

    @Test
    void handleInsufficientFundsException_ShouldReturnBadRequestResponse() {
        // Arrange
        InsufficientFundsException ex = new InsufficientFundsException("Insufficient funds for withdrawal");
        WebRequest request = mock(WebRequest.class);

        // Act
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> responseEntity =
                globalExceptionHandler.handleInsufficientFundsException(ex);

        // Assert
        assertNotNull(responseEntity);
        assertEquals(400, responseEntity.getStatusCodeValue());
        GlobalExceptionHandler.ErrorResponse errorResponse = responseEntity.getBody();
        assertNotNull(errorResponse);
        assertEquals(400, errorResponse.getStatus());
        assertEquals("INSUFFICIENT_FUNDS", errorResponse.getError());
        assertEquals("Insufficient funds for withdrawal", errorResponse.getMessage());
        assertNotNull(errorResponse.getTimestamp());
    }

    @Test
    void handleIllegalArgumentException_ShouldReturnBadRequestResponse() {
        // Arrange
        IllegalArgumentException ex = new IllegalArgumentException("Invalid argument provided");
        WebRequest request = mock(WebRequest.class);

        // Act
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> responseEntity =
                globalExceptionHandler.handleIllegalArgumentException(ex);

        // Assert
        assertNotNull(responseEntity);
        assertEquals(400, responseEntity.getStatusCodeValue());
        GlobalExceptionHandler.ErrorResponse errorResponse = responseEntity.getBody();
        assertNotNull(errorResponse);
        assertEquals(400, errorResponse.getStatus());
        assertEquals("BAD_REQUEST", errorResponse.getError());
        assertEquals("Invalid argument provided", errorResponse.getMessage());
        assertNotNull(errorResponse.getTimestamp());
    }

    @Test
    void handleIllegalStateException_ShouldReturnBadRequestResponse() {
        // Arrange
        IllegalStateException ex = new IllegalStateException("Account is not active for transactions");
        WebRequest request = mock(WebRequest.class);

        // Act
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> responseEntity =
                globalExceptionHandler.handleIllegalStateException(ex);

        // Assert
        assertNotNull(responseEntity);
        assertEquals(400, responseEntity.getStatusCodeValue());
        GlobalExceptionHandler.ErrorResponse errorResponse = responseEntity.getBody();
        assertNotNull(errorResponse);
        assertEquals(400, errorResponse.getStatus());
        assertEquals("BAD_REQUEST", errorResponse.getError());
        assertEquals("Account is not active for transactions", errorResponse.getMessage());
        assertNotNull(errorResponse.getTimestamp());
    }

    @Test
    void handleValidationExceptions_ShouldReturnBadRequestWithFieldErrors() {
        // Arrange
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError1 = new FieldError("accountDto", "accountNumber", "Account number is required");
        FieldError fieldError2 = new FieldError("accountDto", "balance", "Balance must be positive");
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(java.util.Arrays.asList(fieldError1, fieldError2));
        WebRequest request = mock(WebRequest.class);

        // Act
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> responseEntity =
                globalExceptionHandler.handleValidationExceptions(ex);

        // Assert
        assertNotNull(responseEntity);
        assertEquals(400, responseEntity.getStatusCodeValue());
        GlobalExceptionHandler.ErrorResponse errorResponse = responseEntity.getBody();
        assertNotNull(errorResponse);
        assertEquals(400, errorResponse.getStatus());
        assertEquals("VALIDATION_ERROR", errorResponse.getError());
        assertEquals("Validation failed", errorResponse.getMessage());
        assertNotNull(errorResponse.getValidationErrors());
        assertEquals(2, errorResponse.getValidationErrors().size());
        assertEquals("Account number is required", errorResponse.getValidationErrors().get("accountNumber"));
        assertEquals("Balance must be positive", errorResponse.getValidationErrors().get("balance"));
        assertNotNull(errorResponse.getTimestamp());
    }

    @Test
    void handleGenericException_ShouldReturnInternalServerErrorResponse() {
        // Arrange
        Exception ex = new Exception("Unexpected database error");
        WebRequest request = mock(WebRequest.class);

        // Act
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> responseEntity =
                globalExceptionHandler.handleGenericException(ex);

        // Assert
        assertNotNull(responseEntity);
        assertEquals(500, responseEntity.getStatusCodeValue());
        GlobalExceptionHandler.ErrorResponse errorResponse = responseEntity.getBody();
        assertNotNull(errorResponse);
        assertEquals(500, errorResponse.getStatus());
        assertEquals("INTERNAL_SERVER_ERROR", errorResponse.getError());
        assertEquals("An unexpected error occurred", errorResponse.getMessage());
        assertNotNull(errorResponse.getTimestamp());
    }

    @Test
    void handleValidationExceptions_WithEmptyErrors_ShouldReturnEmptyValidationErrors() {
        // Arrange
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(java.util.Collections.emptyList());
        WebRequest request = mock(WebRequest.class);

        // Act
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> responseEntity =
                globalExceptionHandler.handleValidationExceptions(ex);

        // Assert
        assertNotNull(responseEntity);
        assertEquals(400, responseEntity.getStatusCodeValue());
        GlobalExceptionHandler.ErrorResponse errorResponse = responseEntity.getBody();
        assertNotNull(errorResponse);
        assertEquals(400, errorResponse.getStatus());
        assertEquals("VALIDATION_ERROR", errorResponse.getError());
        assertEquals("Validation failed", errorResponse.getMessage());
        assertNotNull(errorResponse.getValidationErrors());
        assertTrue(errorResponse.getValidationErrors().isEmpty());
        assertNotNull(errorResponse.getTimestamp());
    }
}