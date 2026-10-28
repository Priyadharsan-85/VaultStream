package com.vaultstream.auth;

import com.vaultstream.model.AppUser;
import com.vaultstream.security.JwtTokenProvider;
import com.vaultstream.security.LoginAttemptService;
import com.vaultstream.security.TokenRevocationService;
import com.vaultstream.repository.AppUserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenRevocationService tokenRevocationService;
    private final LoginAttemptService loginAttemptService;

    @Value("${vaultstream.security.registration-enabled:true}")
    private boolean registrationEnabled = true;

    @PostMapping("/register")
    public ResponseEntity<AuthDto.LoginResponse> register(@Valid @RequestBody AuthDto.RegisterRequest request) {
        if (!registrationEnabled) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Registration is disabled");
        }

        if (appUserRepository.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already registered");
        }

        AppUser user = appUserRepository.save(AppUser.builder()
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build());
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getUsername(),
                null,
                AuthorityUtils.createAuthorityList("ROLE_USER"));
        String token = jwtTokenProvider.generateToken(authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(new AuthDto.LoginResponse(token, null));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDto.LoginResponse> login(@Valid @RequestBody AuthDto.LoginRequest loginRequest) {
        String username = loginRequest.getUsername();
        if (loginAttemptService.isBlocked(username)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new AuthDto.LoginResponse(null, "Account temporarily locked due to excessive failed login attempts. Please try again later."));
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            username,
                            loginRequest.getPassword()
                    )
            );

            loginAttemptService.loginSucceeded(username);
            String token = jwtTokenProvider.generateToken(authentication);
            return ResponseEntity.ok(new AuthDto.LoginResponse(token, null));
        } catch (AuthenticationException ex) {
            loginAttemptService.loginFailed(username);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthDto.LoginResponse(null, "Invalid credentials"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authorization) {
        String token = authorization.startsWith("Bearer ") ? authorization.substring(7).trim() : "";
        if (token.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "A bearer token is required to log out");
        }
        tokenRevocationService.revoke(token);
        return ResponseEntity.ok().build();
    }
}
