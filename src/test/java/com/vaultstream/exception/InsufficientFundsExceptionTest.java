package com.vaultstream.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import static org.junit.jupiter.api.Assertions.*;

class InsufficientFundsExceptionTest {

    @Test
    void constructor_WithMessage_ShouldSetMessageCorrectly() {
        // Arrange & Act
        String testMessage = "Insufficient funds for withdrawal";
        InsufficientFundsException exception = new InsufficientFundsException(testMessage);

        // Assert
        assertEquals(testMessage, exception.getMessage());
        assertEquals("com.vaultstream.exception.InsufficientFundsException: " + testMessage, exception.toString());
    }

    @Test
    void constructor_WithMessageAndCause_ShouldSetMessageAndCauseCorrectly() {
        // Arrange & Act
        String testMessage = "Insufficient funds for withdrawal";
        Throwable cause = new RuntimeException("Database connection failed");
        InsufficientFundsException exception = new InsufficientFundsException(testMessage, cause);

        // Assert
        assertEquals(testMessage, exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    void shouldHaveResponseStatusBadRequest() {
        // Arrange & Act
        ResponseStatus responseStatus = InsufficientFundsException.class.getAnnotation(ResponseStatus.class);

        // Assert
        assertNotNull(responseStatus);
        assertEquals(HttpStatus.BAD_REQUEST, responseStatus.value());
    }
}
