package com.vaultstream.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyFilterTest {

    private static final String PATH = "/api/transactions/transfer";
    private static final String KEY = "request-key-123";

    @Mock
    private IdempotencyService idempotencyService;

    private IdempotencyFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new IdempotencyFilter(idempotencyService, new ObjectMapper());
        request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI(PATH);
        request.setContentType("application/json");
        request.setContent("{\"account\":1}".getBytes(StandardCharsets.UTF_8));
        request.addHeader("Idempotency-Key", KEY);
        response = new MockHttpServletResponse();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("test-user", "", List.of()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void passesThroughReadRequestsWithoutReserving() throws Exception {
        request.setMethod("GET");
        FilterChain chain = (req, res) -> ((HttpServletResponse) res).setStatus(204);

        filter.doFilter(request, response, chain);

        assertEquals(204, response.getStatus());
        verifyNoInteractions(idempotencyService);
    }

    @Test
    void passesThroughWriteRequestsWithoutIdempotencyKey() throws Exception {
        request.removeHeader("Idempotency-Key");

        filter.doFilter(request, response, (req, res) -> ((HttpServletResponse) res).setStatus(204));

        assertEquals(204, response.getStatus());
        verifyNoInteractions(idempotencyService);
    }

    @Test
    void passesThroughAuthenticationResponsesWithoutCachingBearerTokens() throws Exception {
        request.setRequestURI("/api/auth/register");

        filter.doFilter(request, response, (req, res) -> {
            HttpServletResponse wrapped = (HttpServletResponse) res;
            wrapped.setStatus(201);
            wrapped.setContentType("application/json");
            wrapped.getWriter().write("{\"token\":\"sensitive-bearer-token\"}");
        });

        assertEquals(201, response.getStatus());
        assertEquals("{\"token\":\"sensitive-bearer-token\"}", response.getContentAsString());
        verifyNoInteractions(idempotencyService);
    }

    @Test
    void passesThroughUnrelatedWriteRequestsWithoutCachingTheirResponses() throws Exception {
        request.setRequestURI("/api/accounts");

        filter.doFilter(request, response, (req, res) -> ((HttpServletResponse) res).setStatus(201));

        assertEquals(201, response.getStatus());
        verifyNoInteractions(idempotencyService);
    }

    @Test
    void rejectsKeysLongerThanTheLimit() throws Exception {
        request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI(PATH);
        request.setContentType("application/json");
        request.addHeader("Idempotency-Key", "x".repeat(129));

        filter.doFilter(request, response, (req, res) -> fail("chain should not run"));

        assertEquals(400, response.getStatus());
        verifyNoInteractions(idempotencyService);
    }

    @Test
    void replaysCompletedRequestsWithoutCallingChain() throws Exception {
        when(idempotencyService.reserve(anyString(), eq("POST"), eq(PATH), anyString()))
                .thenReturn(new IdempotencyService.Reservation(
                        IdempotencyService.State.REPLAY, 201, "application/json", "{\"created\":true}"));

        filter.doFilter(request, response, (req, res) -> fail("replay should not call chain"));

        assertEquals(201, response.getStatus());
        assertEquals("application/json", response.getContentType());
        assertEquals("{\"created\":true}", response.getContentAsString());
        verify(idempotencyService).reserve(anyString(), eq("POST"), eq(PATH), anyString());
    }

    @Test
    void rejectsRequestsWhenKeyWasUsedForDifferentRequest() throws Exception {
        when(idempotencyService.reserve(anyString(), eq("POST"), eq(PATH), anyString()))
                .thenReturn(new IdempotencyService.Reservation(IdempotencyService.State.CONFLICT, null, null, null));

        filter.doFilter(request, response, (req, res) -> fail("conflict should not call chain"));

        assertEquals(409, response.getStatus());
        verify(idempotencyService).reserve(anyString(), eq("POST"), eq(PATH), anyString());
    }

    @Test
    void rejectsRequestsWhileSameKeyIsStillProcessing() throws Exception {
        when(idempotencyService.reserve(anyString(), eq("POST"), eq(PATH), anyString()))
                .thenReturn(new IdempotencyService.Reservation(IdempotencyService.State.IN_PROGRESS, null, null, null));

        filter.doFilter(request, response, (req, res) -> fail("in-progress request should not call chain"));

        assertEquals(409, response.getStatus());
    }

    @Test
    void completesReservationAfterSuccessfulResponse() throws Exception {
        when(idempotencyService.reserve(anyString(), eq("POST"), eq(PATH), anyString()))
                .thenReturn(new IdempotencyService.Reservation(IdempotencyService.State.RESERVED, null, null, null));

        filter.doFilter(request, response, (req, res) -> {
            HttpServletResponse wrapped = (HttpServletResponse) res;
            wrapped.setStatus(201);
            wrapped.setContentType("application/json");
            wrapped.getWriter().write("{\"created\":true}");
        });

        assertEquals(201, response.getStatus());
        assertEquals("{\"created\":true}", response.getContentAsString());
        verify(idempotencyService).complete(anyString(), eq(201), eq("application/json"), eq("{\"created\":true}"));
        verify(idempotencyService, never()).remove(anyString());
    }

    @Test
    void removesReservationAfterServerError() throws Exception {
        when(idempotencyService.reserve(anyString(), eq("POST"), eq(PATH), anyString()))
                .thenReturn(new IdempotencyService.Reservation(IdempotencyService.State.RESERVED, null, null, null));

        filter.doFilter(request, response, (req, res) -> ((HttpServletResponse) res).setStatus(500));

        verify(idempotencyService).remove(anyString());
        verify(idempotencyService, never()).complete(anyString(), anyInt(), any(), any());
    }

    @Test
    void removesReservationWhenChainThrows() throws Exception {
        when(idempotencyService.reserve(anyString(), eq("POST"), eq(PATH), anyString()))
                .thenReturn(new IdempotencyService.Reservation(IdempotencyService.State.RESERVED, null, null, null));

        org.junit.jupiter.api.Assertions.assertThrows(IOException.class,
                () -> filter.doFilter(request, response, (req, res) -> { throw new IOException("failed"); }));

        verify(idempotencyService).remove(anyString());
    }
}
