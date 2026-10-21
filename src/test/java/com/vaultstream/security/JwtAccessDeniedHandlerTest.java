package com.vaultstream.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtAccessDeniedHandlerTest {
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final JwtAccessDeniedHandler handler = new JwtAccessDeniedHandler(objectMapper);

    @Test
    void writesForbiddenJsonWithRequestPath() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/accounts/123/withdraw");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(request, response, new AccessDeniedException("role not permitted"));

        Map<?, ?> body = objectMapper.readValue(response.getContentAsString(), Map.class);
        assertEquals(403, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        assertEquals("Forbidden", body.get("error"));
        assertEquals("You do not have permission to access this resource", body.get("message"));
        assertEquals("/api/accounts/123/withdraw", body.get("path"));
        assertNotNull(body.get("timestamp"));
    }
}
