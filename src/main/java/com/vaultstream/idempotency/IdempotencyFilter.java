package com.vaultstream.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.ByteArrayInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@RequiredArgsConstructor
public class IdempotencyFilter implements Filter {

    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";
    private static final int MAX_KEY_LENGTH = 128;

    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        if (!isWriteMethod(httpRequest.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String idempotencyKey = httpRequest.getHeader(IDEMPOTENCY_HEADER);
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            chain.doFilter(request, response);
            return;
        }
        if (idempotencyKey.length() > MAX_KEY_LENGTH) {
            writeConflict(httpResponse, HttpServletResponse.SC_BAD_REQUEST,
                    "Idempotency-Key must be 128 characters or fewer");
            return;
        }

        CachedBodyRequest cachedRequest = new CachedBodyRequest(httpRequest);
        String scopedKey = scopedKey(idempotencyKey);
        String fingerprint = fingerprint(cachedRequest);
        IdempotencyService.Reservation reservation;
        try {
            reservation = idempotencyService.reserve(scopedKey, httpRequest.getMethod(),
                    httpRequest.getRequestURI(), fingerprint);
        } catch (DataIntegrityViolationException concurrentRequest) {
            reservation = idempotencyService.lookup(scopedKey, httpRequest.getMethod(),
                    httpRequest.getRequestURI(), fingerprint);
        }

        if (reservation.state() == IdempotencyService.State.REPLAY) {
            writeReplay(httpResponse, reservation);
            return;
        }
        if (reservation.state() == IdempotencyService.State.CONFLICT) {
            writeConflict(httpResponse, HttpServletResponse.SC_CONFLICT,
                    "This idempotency key was already used for a different request");
            return;
        }
        if (reservation.state() == IdempotencyService.State.IN_PROGRESS) {
            writeConflict(httpResponse, HttpServletResponse.SC_CONFLICT,
                    "A request with this idempotency key is still processing");
            return;
        }

        ContentCachingResponseWrapper capturedResponse = new ContentCachingResponseWrapper(httpResponse);
        try {
            chain.doFilter(cachedRequest, capturedResponse);
            int status = capturedResponse.getStatus();
            if (status < HttpServletResponse.SC_INTERNAL_SERVER_ERROR) {
                idempotencyService.complete(scopedKey, status, capturedResponse.getContentType(),
                        new String(capturedResponse.getContentAsByteArray(), StandardCharsets.UTF_8));
            } else {
                idempotencyService.remove(scopedKey);
            }
        } catch (IOException | ServletException | RuntimeException ex) {
            idempotencyService.remove(scopedKey);
            throw ex;
        } finally {
            capturedResponse.copyBodyToResponse();
        }
    }

    private boolean isWriteMethod(String method) {
        return "POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method);
    }

    private String scopedKey(String key) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication == null ? "anonymous" : authentication.getName();
        return sha256(username + "\u0000" + key);
    }

    private String fingerprint(HttpServletRequest request) {
        try {
            byte[] body = StreamUtils.copyToByteArray(request.getInputStream());
            String bodyHash = sha256(body);
            return request.getMethod() + "\n" + request.getRequestURI() + "\n"
                    + (request.getQueryString() == null ? "" : request.getQueryString()) + "\n" + bodyHash;
        } catch (IOException ex) {
            throw new IllegalStateException("Could not read request body for idempotency", ex);
        }
    }

    private String sha256(String value) {
        return sha256(value.getBytes(StandardCharsets.UTF_8));
    }

    private String sha256(byte[] value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private void writeReplay(HttpServletResponse response, IdempotencyService.Reservation reservation)
            throws IOException {
        response.setStatus(reservation.responseCode());
        if (reservation.contentType() != null) {
            response.setContentType(reservation.contentType());
        }
        response.getOutputStream().write(reservation.responseBody().getBytes(StandardCharsets.UTF_8));
    }

    private void writeConflict(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        objectMapper.writeValue(response.getOutputStream(), new ConflictResponse(status, message));
    }

    private record ConflictResponse(int status, String message) { }

    private static class CachedBodyRequest extends HttpServletRequestWrapper {
        private final byte[] body;

        private CachedBodyRequest(HttpServletRequest request) throws IOException {
            super(request);
            this.body = StreamUtils.copyToByteArray(request.getInputStream());
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream input = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public int read() {
                    return input.read();
                }

                @Override
                public boolean isFinished() {
                    return input.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    throw new UnsupportedOperationException("Asynchronous request reading is not supported");
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            Charset charset = getCharacterEncoding() == null
                    ? StandardCharsets.UTF_8
                    : Charset.forName(getCharacterEncoding());
            return new BufferedReader(new InputStreamReader(getInputStream(), charset));
        }
    }
}
