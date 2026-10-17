package com.vaultstream.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class IdempotencyFilter implements Filter {

    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    // Methods that should be idempotent (typically POST, PUT, PATCH)
    private static final String[] IDEMPOTENT_METHODS = {"POST", "PUT", "PATCH"};

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Only apply idempotency check to specific methods
        if (isIdempotentMethod(httpRequest.getMethod())) {
            String idempotencyKey = httpRequest.getHeader("Idempotency-Key");

            if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                // Generate cache key based on method, path, query params, and body
                String cacheKey = generateCacheKey(httpRequest, idempotencyKey);

                // Check if we have a cached response
                Optional<String> cachedResponse = idempotencyService.getResponseIfExists(
                        cacheKey,
                        httpRequest.getMethod(),
                        httpRequest.getRequestURI(),
                        getQueryString(httpRequest)
                );

                if (cachedResponse.isPresent()) {
                    // Return cached response
                    log.info("Returning cached response for idempotency key: {}", idempotencyKey);
                    httpResponse.setStatus(200); // Assuming cached responses are successful
                    httpResponse.setContentType("application/json");
                    httpResponse.getWriter().write(cachedResponse.get());
                    return;
                }

                // Wrap response to capture it for caching
                CachedResponseWrapper responseWrapper = new CachedResponseWrapper(httpResponse);
                chain.doFilter(request, responseWrapper);

                // Cache the response
                String responseBody = responseWrapper.getResponseBody();
                if (responseWrapper.getStatus() < 500) { // Only cache successful responses
                    idempotencyService.saveResponse(
                            cacheKey,
                            httpRequest.getMethod(),
                            httpRequest.getRequestURI(),
                            getQueryString(httpRequest),
                            responseWrapper.getStatus(),
                            responseBody
                    );
                    log.info("Cached response for idempotency key: {}", idempotencyKey);
                }
            } else {
                // No idempotency key provided, continue normally
                chain.doFilter(request, response);
            }
        } else {
            // Not an idempotent method, continue normally
            chain.doFilter(request, response);
        }
    }

    private boolean isIdempotentMethod(String method) {
        for (String idempotentMethod : IDEMPOTENT_METHODS) {
            if (idempotentMethod.equalsIgnoreCase(method)) {
                return true;
            }
        }
        return false;
    }

    private String generateCacheKey(HttpServletRequest request, String idempotencyKey) {
        // Combine idempotency key with method and path to create unique key
        return String.format("%s:%s:%s",
                idempotencyKey,
                request.getMethod(),
                request.getRequestURI());
    }

    private String getQueryString(HttpServletRequest request) {
        Map<String, String[]> params = request.getParameterMap();
        if (params.isEmpty()) {
            return "";
        }

        StringBuilder queryString = new StringBuilder();
        for (Map.Entry<String, String[]> entry : params.entrySet()) {
            queryString.append(entry.getKey()).append("=");
            String[] values = entry.getValue();
            for (int i = 0; i < values.length; i++) {
                queryString.append(values[i]);
                if (i < values.length - 1) {
                    queryString.append(",");
                }
            }
            queryString.append("&");
        }
        // Remove trailing &
        if (queryString.length() > 0) {
            queryString.setLength(queryString.length() - 1);
        }
        return queryString.toString();
    }

    // Wrapper to capture response body
    private static class CachedResponseWrapper extends HttpServletResponseWrapper {
        private final StringBuilder responseBodyBuilder = new StringBuilder();
        private int status = HttpServletResponse.SC_OK;

        public CachedResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            return new CachedServletOutputStream(responseBodyBuilder);
        }

        @Override
        public void setStatus(int statusCode) {
            this.status = statusCode;
            super.setStatus(statusCode);
        }

        public int getStatus() {
            return status;
        }

        public String getResponseBody() {
            return responseBodyBuilder.toString();
        }

        private static class CachedServletOutputStream extends ServletOutputStream {
            private final StringBuilder builder;

            public CachedServletOutputStream(StringBuilder builder) {
                this.builder = builder;
            }

            @Override
            public void write(int b) throws IOException {
                builder.append((char) b);
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(WriteListener writeListener) {
                // Not needed for this implementation
            }
        }
    }
}