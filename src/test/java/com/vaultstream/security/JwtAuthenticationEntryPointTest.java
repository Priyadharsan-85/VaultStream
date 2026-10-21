package com.vaultstream.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtAuthenticationEntryPointTest {
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final JwtAuthenticationEntryPoint entryPoint = new JwtAuthenticationEntryPoint(objectMapper);

    @Test
    void writesUnauthorizedJsonWithAuthenticationMessageAndPath() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/accounts");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException("Invalid credentials"));

        Map<?, ?> body = objectMapper.readValue(response.getContentAsString(), Map.class);
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        assertEquals("Unauthorized", body.get("error"));
        assertEquals("Authentication failed: Invalid credentials", body.get("message"));
        assertEquals("/api/accounts", body.get("path"));
        assertNotNull(body.get("timestamp"));
    }
}
