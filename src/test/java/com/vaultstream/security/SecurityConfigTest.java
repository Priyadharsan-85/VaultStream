package com.vaultstream.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        securityConfig = new SecurityConfig(
                jwtTokenProvider, userDetailsService, authenticationEntryPoint, accessDeniedHandler);
    }

    @Test
    void passwordEncoderUsesBCrypt() {
        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();

        assertInstanceOf(BCryptPasswordEncoder.class, passwordEncoder);
    }

    @Test
    void authenticationManagerComesFromSpringConfiguration() throws Exception {
        AuthenticationConfiguration authenticationConfiguration = org.mockito.Mockito.mock(AuthenticationConfiguration.class);
        AuthenticationManager authenticationManager = org.mockito.Mockito.mock(AuthenticationManager.class);
        when(authenticationConfiguration.getAuthenticationManager()).thenReturn(authenticationManager);

        assertSame(authenticationManager, securityConfig.authenticationManager(authenticationConfiguration));
        verify(authenticationConfiguration).getAuthenticationManager();
    }

    @Test
    void createsJwtAuthenticationFilterWithConfiguredDependencies() {
        assertInstanceOf(JwtAuthenticationFilter.class, securityConfig.jwtAuthenticationFilter());
    }
}
