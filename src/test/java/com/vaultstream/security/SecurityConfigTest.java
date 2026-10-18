package com.vaultstream.security;

import com.vaultstream.exception.GlobalExceptionHandler;
import com.vaultstream.security.JwtAccessDeniedHandler;
import com.vaultstream.security.JwtAuthenticationEntryPoint;
import com.vaultstream.security.JwtAuthenticationFilter;
import com.vaultstream.security.JwtTokenProvider;
import com.vaultstream.security.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @Mock
    private JwtAuthenticationEntryPoint authenticationEntryPoint;

    @Mock
    private JwtAccessDeniedHandler accessDeniedHandler;

    @Mock
    private AuthenticationConfiguration authenticationConfiguration;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private SecurityConfig securityConfig;

    @Test
    void passwordEncoder_ShouldReturnBCryptPasswordEncoder() {
        // Act
        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();

        // Assert
        assertNotNull(passwordEncoder);
        assertTrue(passwordEncoder instanceof BCryptPasswordEncoder);
    }

    @Test
    void authenticationManager_ShouldReturnAuthenticationManagerFromConfiguration() throws Exception {
        // Arrange
        when(authenticationConfiguration.getAuthenticationManager()).thenReturn(authenticationManager);

        // Act
        AuthenticationManager result = securityConfig.authenticationManager(authenticationConfiguration);

        // Assert
        assertSame(authenticationManager, result);
        verify(authenticationConfiguration).getAuthenticationManager();
    }

    @Test
    void jwtAuthenticationFilter_ShouldReturnJwtAuthenticationFilterInstance() {
        // Act
        JwtAuthenticationFilter filter = securityConfig.jwtAuthenticationFilter();

        // Assert
        assertNotNull(filter);
        // Note: We can't easily verify the constructor arguments without mocking the constructor
        // But we can verify it's the right type
        assertTrue(filter instanceof JwtAuthenticationFilter);
    }

    @Test
    void securityFilterChain_ShouldConfigureHttpSecurityCorrectly() throws Exception {
        // Arrange
        @SuppressWarnings("unchecked")
        var http = mock(org.springframework.security.config.annotation.web.builders.HttpSecurity.class);
        var csrf = mock(org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer.class);
        var exceptionHandling = mock(org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer.class);
        var sessionManagement = mock(org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer.class);
        var authorizeHttpRequests = mock(org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer.class);
        var httpSecurityBuilder = mock(org.springframework.security.config.annotation.web.builders.HttpSecurity.class);

        when(http.csrf()).thenReturn(csrf);
        when(csrf.disable()).thenReturn(http);
        when(http.exceptionHandling()).thenReturn(exceptionHandling);
        when(http.sessionManagement()).thenReturn(sessionManagement);
        when(http.authorizeHttpRequests()).thenReturn(authorizeHttpRequests);

        // Mock the exception handling chain
        when(exceptionHandling.authenticationPoint(any(JwtAuthenticationEntryPoint.class))).thenReturn(http);
        when(exceptionHandling.accessDeniedHandler(any(JwtAccessDeniedHandler.class))).thenReturn(http);

        // Mock the session management chain
        when(sessionManagement.sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.STATELESS)).thenReturn(http);

        // Mock the authorization chain
        when(authorizeHttpRequests.requestMatchers(anyString())).thenReturn(authorizeHttpRequests);
        when(authorizeHttpRequests.permitAll()).thenReturn(authorizeHttpRequests);
        when(authorizeHttpRequests.anyRequest()).thenReturn(authorizeHttpRequests);
        when(authorizeHttpRequests.authenticated()).thenReturn(authorizeHttpRequests);

        // Mock the filter addition
        when(http.addFilterBefore(any(JwtAuthenticationFilter.class), any())).thenReturn(http);

        // Mock the build method
        when(http.build()).thenReturn(mock(SecurityFilterChain.class));

        // Act
        SecurityFilterChain result = securityConfig.securityFilterChain(http);

        // Assert
        assertNotNull(result);

        // Verify key method calls were made
        verify(http).csrf();
        verify(csrf).disable();
        verify(http).exceptionHandling();
        verify(http).sessionManagement();
        verify(http).authorizeHttpRequests();
        verify(http).addFilterBefore(any(JwtAuthenticationFilter.class), eq(UsernamePasswordAuthenticationFilter.class));
        verify(http).build();
    }
}