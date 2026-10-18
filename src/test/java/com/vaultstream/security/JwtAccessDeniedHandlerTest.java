package com.vaultstream.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAccessDeniedHandlerTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private AccessDeniedException accessDeniedException;

    @InjectMocks
    private JwtAccessDeniedHandler accessDeniedHandler;

    private StringWriter stringWriter;
    private PrintWriter printWriter;

    @BeforeEach
    void setUp() throws Exception {
        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);
        when(request.getRequestURI()).thenReturn("/api/accounts/123/withdraw");
        when(accessDeniedException.getMessage()).thenReturn("Access is denied");
    }

    @Test
    void handle_ShouldSetResponseStatusAndContentType_WhenCalled() throws Exception {
        // Act
        accessDeniedHandler.handle(request, response, accessDeniedException);

        // Assert
        verify(response).setContentType("application/json");
        verify(response).setStatus(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void handle_ShouldWriteAccessDeniedResponse_WhenCalled() throws Exception {
        // Act
        accessDeniedHandler.handle(request, response, accessDeniedException);

        // Assert
        verify(objectMapper).writeValue(eq(printWriter), any(Map.class));
        printWriter.flush(); // Ensure all content is written

        String jsonResponse = stringWriter.toString();
        assertFalse(jsonResponse.isEmpty(), "JSON response should not be empty");
        assertTrue(jsonResponse.contains("\"timestamp\":"), "Should contain timestamp");
        assertTrue(jsonResponse.contains("\"status\":403"), "Should contain status 403");
        assertTrue(jsonResponse.contains("\"error\":\"Forbidden\""), "Should contain error Forbidden");
        assertTrue(jsonResponse.contains("\"message\":\"You do not have permission to access this resource\""), "Should contain error message");
        assertTrue(jsonResponse.contains("\"path\":\"/api/accounts/123/withdraw\""), "Should contain request path");
    }

    @Test
    void handle_ShouldHandleDifferentExceptionMessages_StillReturnSameMessage() throws Exception {
        // Arrange - The AccessDeniedHandler ignores the exception message and uses a static message
        when(accessDeniedException.getMessage()).thenReturn("Custom access denied message");

        // Act
        accessDeniedHandler.handle(request, response, accessDeniedException);

        // Assert
        printWriter.flush();
        String jsonResponse = stringWriter.toString();
        // Should still contain the static message, not the exception message
        assertTrue(jsonResponse.contains("\"message\":\"You do not have permission to access this resource\""));
        assertFalse(jsonResponse.contains("Custom access denied message"));
    }

    @Test
    void handle_ShouldHandleDifferentRequestPaths() throws Exception {
        // Arrange
        when(request.getRequestURI()).thenReturn("/api/transactions/transfer");

        // Act
        accessDeniedHandler.handle(request, response, accessDeniedException);

        // Assert
        printWriter.flush();
        String jsonResponse = stringWriter.toString();
        assertTrue(jsonResponse.contains("\"path\":\"/api/transactions/transfer\""));
    }

    @Test
    void handle_ShouldHandleIOExceptionGracefully() throws Exception {
        // Arrange
        doThrow(new IOException("Writer error")).when(objectMapper).writeValue(any(PrintWriter.class), any());

        // Act & Assert
        assertThrows(IOException.class, () -> accessDeniedHandler.handle(request, response, accessDeniedException));

        // Verify that status and content type were still set before the exception
        verify(response).setContentType("application/json");
        verify(response).setStatus(HttpStatus.FORBIDDEN.value());
    }
}