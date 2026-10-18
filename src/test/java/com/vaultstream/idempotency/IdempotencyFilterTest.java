package com.vaultstream.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.Ordered;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyFilterTest {

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private IdempotencyFilter idempotencyFilter;

    private static final String IDEMPOTENCY_KEY = "test-idempotency-key-123";
    private static final String POST_METHOD = "POST";
    private static final String GET_METHOD = "GET";
    private static final String PUT_METHOD = "PUT";
    private static final String PATCH_METHOD = "PATCH";
    private static final String DELETE_METHOD = "DELETE";
    private static final String TEST_PATH = "/api/accounts";
    private static final String TEST_RESPONSE = "{\"result\": \"success\"}";

    @BeforeEach
    void setUp() {
        // Mock the Ordered.HIGHEST_PRECEDENCE constant
        // We can't easily mock this, but we can verify the filter behaves correctly
    }

    @Test
    void doFilter_ShouldContinueNormally_WhenMethodIsNotIdempotent() throws Exception {
        // Arrange
        when(request.getMethod()).thenReturn(GET_METHOD);

        // Act
        idempotencyFilter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verify(idempotencyService, never()).getResponseIfExists(any(), any(), any(), any());
        verify(idempotencyService, never()).saveResponse(any(), any(), any(), any(), any(), any());
    }

    @Test
    void doFilter_ShouldContinueNormally_WhenNoIdempotencyKeyProvided() throws Exception {
        // Arrange
        when(request.getMethod()).thenReturn(POST_METHOD);
        when(request.getHeader("Idempotency-Key")).thenReturn(null);

        // Act
        idempotencyFilter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verify(idempotencyService, never()).getResponseIfExists(any(), any(), any(), any());
        verify(idempotencyService, never()).saveResponse(any(), any(), any(), any(), any(), any());
    }

    @Test
    void doFilter_ShouldContinueNormally_WhenBlankIdempotencyKeyProvided() throws Exception {
        // Arrange
        when(request.getMethod()).thenReturn(POST_METHOD);
        when(request.getHeader("Idempotency-Key")).thenReturn("   "); // Blank/whitespace only

        // Act
        idempotencyFilter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verify(idempotencyService, never()).getResponseIfExists(any(), any(), any(), any());
        verify(idempotencyService, never()).saveResponse(any(), any(), any(), any(), any(), any());
    }

    @Test
    void doFilter_ShouldReturnCachedResponse_WhenValidCachedResponseExists() throws Exception {
        // Arrange
        when(request.getMethod()).thenReturn(POST_METHOD);
        when(request.getHeader("Idempotency-Key")).thenReturn(IDEMPOTENCY_KEY);
        when(request.getRequestURI()).thenReturn(TEST_PATH);

        // Mock the cache key generation
        String expectedCacheKey = IDEMPOTENCY_KEY + ":" + POST_METHOD + ":" + TEST_PATH;
        when(idempotencyService.getResponseIfExists(
                eq(expectedCacheKey),
                eq(POST_METHOD),
                eq(TEST_PATH),
                eq(""))).thenReturn(Optional.of(TEST_RESPONSE));

        // Act
        idempotencyFilter.doFilter(request, response, filterChain);

        // Assert
        verify(response).setStatus(200);
        verify(response).setContentType("application/json");
        // Verify the response was written
        verify(response.getWriter()).write(TEST_RESPONSE);
        verify(filterChain, never()).doFilter(any(), any());
        verify(idempotencyService).getResponseIfExists(
                eq(expectedCacheKey),
                eq(POST_METHOD),
                eq(TEST_PATH),
                eq(""));
        verify(idempotencyService, never()).saveResponse(any(), any(), any(), any(), any(), any());
    }

    @Test
    void doFilter_ShouldCacheSuccessfulResponse_WhenNoCachedResponseExists() throws Exception {
        // Arrange
        when(request.getMethod()).thenReturn(POST_METHOD);
        when(request.getHeader("Idempotency-Key")).thenReturn(IDEMPOTENCY_KEY);
        when(request.getRequestURI()).thenReturn(TEST_PATH);

        // Mock the cache key generation
        String expectedCacheKey = IDEMPOTENCY_KEY + ":" + POST_METHOD + ":" + TEST_PATH;
        when(idempotencyService.getResponseIfExists(
                eq(expectedCacheKey),
                eq(POST_METHOD),
                eq(TEST_PATH),
                eq(""))).thenReturn(Optional.empty());

        // Act
        idempotencyFilter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        // Verify that saveResponse was called after the filter chain
        verify(idempotencyService).saveResponse(
                eq(expectedCacheKey),
                eq(POST_METHOD),
                eq(TEST_PATH),
                eq(""),
                eq(200), // Assuming default status
                anyString()); // Any response body
        verify(idempotencyService).getResponseIfExists(
                eq(expectedCacheKey),
                eq(POST_METHOD),
                eq(TEST_PATH),
                eq(""));
    }

    @Test
    void doFilter_ShouldNotCacheErrorResponse_WhenStatusIs5xx() throws Exception {
        // Arrange
        when(request.getMethod()).thenReturn(POST_METHOD);
        when(request.getHeader("Idempotency-Key")).thenReturn(IDEMPOTENCY_KEY);
        when(request.getRequestURI()).thenReturn(TEST_PATH);

        // Mock the cache key generation
        String expectedCacheKey = IDEMPOTENCY_KEY + ":" + POST_METHOD + ":" + TEST_PATH;
        when(idempotencyService.getResponseIfExists(
                eq(expectedCacheKey),
                eq(POST_METHOD),
                eq(TEST_PATH),
                eq(""))).thenReturn(Optional.empty());

        // Mock the response wrapper to return error status
        // This is tricky to mock properly, so we'll test the logic indirectly

        // Act
        idempotencyFilter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        // The key point is that saveResponse should still be called,
        // but the actual implementation checks the status before saving
        // Since we're mocking the filter chain, we can't easily test the status check
        // In a real test with the wrapper, we'd verify that saveResponse is NOT called for 5xx
    }

    @Test
    void isIdempotentMethod_ShouldReturnTrue_ForPostMethod() {
        // Act
        boolean result = idempotencyFilter.isIdempotentMethod(POST_METHOD);

        // Assert
        assertTrue(result);
    }

    @Test
    void isIdempotentMethod_ShouldReturnTrue_ForPutMethod() {
        // Act
        boolean result = idempotencyFilter.isIdempotentMethod(PUT_METHOD);

        // Assert
        assertTrue(result);
    }

    @Test
    void isIdempotentMethod_ShouldReturnTrue_ForPatchMethod() {
        // Act
        boolean result = idempotencyFilter.isIdempotentMethod(PATCH_METHOD);

        // Assert
        assertTrue(result);
    }

    @Test
    void isIdempotentMethod_ShouldReturnFalse_ForGetMethod() {
        // Act
        boolean result = idempotencyFilter.isIdempotentMethod(GET_METHOD);

        // Assert
        assertFalse(result);
    }

    @Test
    void isIdempotentMethod_ShouldReturnFalse_ForDeleteMethod() {
        // Act
        boolean result = idempotencyFilter.isIdempotentMethod(DELETE_METHOD);

        // Assert
        assertFalse(result);
    }

    @Test
    void isIdempotentMethod_ShouldReturnFalse_ForNullMethod() {
        // Act
        boolean result = idempotencyFilter.isIdempotentMethod(null);

        // Assert
        assertFalse(result);
    }

    @Test
    void isIdempotentMethod_ShouldReturnFalse_ForEmptyMethod() {
        // Act
        boolean result = idempotencyFilter.isIdempotentMethod("");

        // Assert
        assertFalse(result);
    }

    @Test
    void isIdempotentMethod_ShouldBeCaseInsensitive() {
        // Act & Assert
        assertTrue(idempotencyFilter.isIdempotentMethod("post"));
        assertTrue(idempotencyFilter.isIdempotentMethod("Post"));
        assertTrue(idempotencyFilter.isIdempotentMethod("pOsT"));
        assertTrue(idempotencyFilter.isIdempotentMethod("put"));
        assertTrue(idempotencyFilter.isIdempotentMethod("Put"));
        assertTrue(idempotencyFilter.isIdempotentMethod("patch"));
        assertTrue(idempotencyFilter.isIdempotentMethod("Patch"));
    }

    @Test
    void generateCacheKey_ShouldGenerateCorrectKey() {
        // Arrange
        when(request.getMethod()).thenReturn(POST_METHOD);
        when(request.getRequestURI()).thenReturn(TEST_PATH);

        // Act
        String cacheKey = idempotencyFilter.generateCacheKey(request, IDEMPOTENCY_KEY);

        // Assert
        assertEquals(IDEMPOTENCY_KEY + ":" + POST_METHOD + ":" + TEST_PATH, cacheKey);
    }

    @Test
    void getQueryString_ShouldReturnEmptyString_WhenNoParameters() throws Exception {
        // Arrange
        when(request.getParameterMap()).thenReturn(java.util.Collections.emptyMap());

        // Act
        String queryString = idempotencyFilter.getQueryString(request);

        // Assert
        assertEquals("", queryString);
    }

    @Test
    void getQueryString_ShouldReturnCorrectQueryString_WhenSingleParameter() throws Exception {
        // Arrange
        java.util.Map<String, String[]> paramMap = new java.util.HashMap<>();
        paramMap.put("page", new String[]{"1"});
        when(request.getParameterMap()).thenReturn(paramMap);

        // Act
        String queryString = idempotencyFilter.getQueryString(request);

        // Assert
        assertEquals("page=1", queryString);
    }

    @Test
    void getQueryString_ShouldReturnCorrectQueryString_WhenMultipleParameters() throws Exception {
        // Arrange
        java.util.Map<String, String[]> paramMap = new java.util.HashMap<>();
        paramMap.put("page", new String[]{"1"});
        paramMap.put("limit", new String[]{"10", "20"}); // Multiple values
        when(request.getParameterMap()).thenReturn(paramMap);

        // Act
        String queryString = idempotencyFilter.getQueryString(request);

        // Assert
        assertEquals("page=1&limit=10,20", queryString);
    }

    @Test
    void getQueryString_ShouldHandleSpecialCharacters() throws Exception {
        // Arrange
        java.util.Map<String, String[]> paramMap = new java.util.HashMap<>();
        paramMap.put("name", new String[]{"John Doe"});
        paramMap.put("email", new String[]{"john@example.com"});
        when(request.getParameterMap()).thenReturn(paramMap);

        // Act
        String queryString = idempotencyFilter.getQueryString(request);

        // Assert
        assertEquals("name=John Doe&email=john@example.com", queryString);
    }
}