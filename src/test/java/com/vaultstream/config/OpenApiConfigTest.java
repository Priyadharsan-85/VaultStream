package com.vaultstream.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.*;

class OpenApiConfigTest {

    @Test
    void openApiConfig_shouldCreateOpenApiBeanWithCorrectInfo() {
        // Arrange & Act
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(OpenApiConfig.class)) {
            OpenAPI openApi = context.getBean(OpenAPI.class);

            // Assert
            assertNotNull(openApi);
            Info info = openApi.getInfo();
            assertNotNull(info);
            assertEquals("VaultStream API", info.getTitle());
            assertEquals("Real-Time Event-Driven Ledger System API", info.getDescription());
            assertEquals("1.0.0", info.getVersion());
        }
    }

    @Test
    void openApiConfig_shouldIncludeBearerAuthSecurityScheme() {
        // Arrange & Act
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(OpenApiConfig.class)) {
            OpenAPI openApi = context.getBean(OpenAPI.class);

            // Assert
            assertNotNull(openApi.getComponents());
            SecurityScheme securityScheme = openApi.getComponents().getSecuritySchemes().get("bearerAuth");
            assertNotNull(securityScheme);
            assertEquals(SecurityScheme.Type.HTTP, securityScheme.getType());
            assertEquals("bearer", securityScheme.getScheme());
            assertEquals("JWT", securityScheme.getBearerFormat());

            // Check that security requirement is present
            assertFalse(openApi.getSecurity().isEmpty());
            SecurityRequirement securityRequirement = openApi.getSecurity().get(0);
            assertTrue(securityRequirement.get("bearerAuth") != null && securityRequirement.get("bearerAuth").isEmpty());
        }
    }

    @Test
    void openApiConfig_shouldCreateGroupedOpenApiBean() {
        // Arrange & Act
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(OpenApiConfig.class)) {
            GroupedOpenApi groupedOpenApi = context.getBean(GroupedOpenApi.class);

            // Assert
            assertNotNull(groupedOpenApi);
            assertEquals("public", groupedOpenApi.getGroup());
        }
    }
}
