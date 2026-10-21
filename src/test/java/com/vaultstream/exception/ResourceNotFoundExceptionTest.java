package com.vaultstream.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import static org.junit.jupiter.api.Assertions.*;

class ResourceNotFoundExceptionTest {

    @Test
    void constructor_WithMessage_ShouldSetMessageCorrectly() {
        // Arrange & Act
        String testMessage = "Account not found with id: 123";
        ResourceNotFoundException exception = new ResourceNotFoundException(testMessage);

        // Assert
        assertEquals(testMessage, exception.getMessage());
        assertEquals("com.vaultstream.exception.ResourceNotFoundException: " + testMessage, exception.toString());
    }

    @Test
    void constructor_WithMessageAndCause_ShouldSetMessageAndCauseCorrectly() {
        // Arrange & Act
        String testMessage = "Account not found with id: 123";
        Throwable cause = new RuntimeException("Database query failed");
        ResourceNotFoundException exception = new ResourceNotFoundException(testMessage, cause);

        // Assert
        assertEquals(testMessage, exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    void shouldHaveResponseStatusNotFound() {
        // Arrange & Act
        ResponseStatus responseStatus = ResourceNotFoundException.class.getAnnotation(ResponseStatus.class);

        // Assert
        assertNotNull(responseStatus);
        assertEquals(HttpStatus.NOT_FOUND, responseStatus.value());
    }
}
