package com.vaultstream.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaultstream.auth.AuthDto.LoginRequest;
import com.vaultstream.auth.AuthDto.LoginResponse;
import com.vaultstream.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private com.vaultstream.service.AccountService accountService;

    @InjectMocks
    private AuthController authController;

    private LoginRequest validLoginRequest;
    private LoginRequest invalidLoginRequest;
    private Authentication mockAuthentication;

    @BeforeEach
    void setUp() {
        validLoginRequest = new LoginRequest("testuser", "testpass");
        invalidLoginRequest = new LoginRequest("invaliduser", "invalidpass");
        mockAuthentication = mock(Authentication.class);
    }

    @Test
    void login_ShouldReturnToken_WhenValidCredentialsProvided() throws Exception {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuthentication);
        when(jwtTokenProvider.generateToken(mockAuthentication)).thenReturn("test-jwt-token");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("test-jwt-token"))
                .andExpect(jsonPath("$.errorMessage").isEmpty());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtTokenProvider).generateToken(mockAuthentication);
    }

    @Test
    void login_ShouldReturnError_WhenInvalidCredentialsProvided() throws Exception {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new RuntimeException("Invalid credentials"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidLoginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.token").isEmpty())
                .andExpect(jsonPath("$.errorMessage").value("Invalid credentials"));

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void login_ShouldReturnError_WhenUsernameIsNull() throws Exception {
        // Arrange
        LoginRequest requestWithNullUsername = new LoginRequest(null, "testpass");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestWithNullUsername)))
                .andExpect(status().isBadRequest()); // Validation failure

        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void login_ShouldReturnError_WhenPasswordIsNull() throws Exception {
        // Arrange
        LoginRequest requestWithNullPassword = new LoginRequest("testuser", null);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestWithNullPassword)))
                .andExpect(status().isBadRequest()); // Validation failure

        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void login_ShouldReturnError_WhenUsernameIsEmpty() throws Exception {
        // Arrange
        LoginRequest requestWithEmptyUsername = new LoginRequest("", "testpass");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestWithEmptyUsername)))
                .andExpect(status().isBadRequest()); // Validation failure

        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void login_ShouldReturnError_WhenPasswordIsEmpty() throws Exception {
        // Arrange
        LoginRequest requestWithEmptyPassword = new LoginRequest("testuser", "");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestWithEmptyPassword)))
                .andExpect(status().isBadRequest()); // Validation failure

        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void logout_ShouldReturnOkResponse() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk());
    }
}