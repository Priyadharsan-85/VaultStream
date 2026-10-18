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
import org.springframework.security.core.AuthenticationException;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationEntryPointTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private AuthenticationException authException;

    @InjectMocks
    private JwtAuthenticationEntryPoint authenticationEntryPoint;

    private StringWriter stringWriter;
    private PrintWriter printWriter;

    @BeforeEach
    void setUp() throws Exception {
        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);
        when(request.getRequestURI()).thenReturn("/api/accounts/123");
        when(authException.getMessage()).thenReturn("Invalid credentials");
    }

    @Test
    void commence_ShouldSetResponseStatusAndContentType_WhenCalled() throws Exception {
        // Act
        authenticationEntryPoint.commence(request, response, authException);

        // Assert
        verify(response).setContentType("application/json");
        verify(response).setStatus(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void commence_ShouldWriteErrorResponse_WhenCalled() throws Exception {
        // Act
        authenticationEntryPoint.commence(request, response, authException);

        // Assert
        verify(objectMapper).writeValue(eq(printWriter), any(Map.class));
        printWriter.flush(); // Ensure all content is written

        String jsonResponse = stringWriter.toString();
        assertFalse(jsonResponse.isEmpty(), "JSON response should not be empty");
        assertTrue(jsonResponse.contains("\"timestamp\":"), "Should contain timestamp");
        assertTrue(jsonResponse.contains("\"status\":401"), "Should contain status 401");
        assertTrue(jsonResponse.contains("\"error\":\"Unauthorized\""), "Should contain error Unauthorized");
        assertTrue(jsonResponse.contains("\"message\":\"Authentication failed: Invalid credentials\""), "Should contain error message");
        assertTrue(jsonResponse.contains("\"path\":\"/api/accounts/123\""), "Should contain request path");
    }

    @Test
    void commence_ShouldHandleDifferentExceptionMessages() throws Exception {
        // Arrange
        when(authException.getMessage()).thenReturn("Token expired");

        // Act
        authenticationEntryPoint.commence(request, response, authException);

        // Assert
        printWriter.flush();
        String jsonResponse = stringWriter.toString();
        assertTrue(jsonResponse.contains("\"message\":\"Authentication failed: Token expired\""));
    }

    @Test
    void commence_ShouldHandleDifferentRequestPaths() throws Exception {
        // Arrange
        when(request.getRequestURI()).thenReturn("/api/transactions/deposit");

        // Act
        authenticationEntryPoint.commence(request, response, authException);

        // Assert
        printWriter.flush();
        String jsonResponse = stringWriter.toString();
        assertTrue(jsonResponse.contains("\"path\":\"/api/transactions/deposit\""));
    }

    @Test
    void commence_ShouldHandleIOExceptionGracefully() throws Exception {
        // Arrange
        doThrow(new IOException("Writer error")).when(objectMapper).writeValue(any(PrintWriter.class), any());

        // Act & Assert
        assertThrows(IOException.class, () -> authenticationEntryPoint.commence(request, response, authException));

        // Verify that status and content type were still set before the exception
        verify(response).setContentType("application/json");
        verify(response).setStatus(HttpStatus.UNAUTHORIZED.value());
    }
}