package com.vaultstream.auth;

import com.vaultstream.model.AppUser;
import com.vaultstream.security.JwtTokenProvider;
import com.vaultstream.repository.AppUserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    @PostMapping("/register")
    public ResponseEntity<AuthDto.LoginResponse> register(@Valid @RequestBody AuthDto.RegisterRequest request) {
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
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            String token = jwtTokenProvider.generateToken(authentication);
            return ResponseEntity.ok(new AuthDto.LoginResponse(token, null));
        } catch (AuthenticationException ex) {
            return ResponseEntity.status(401).body(new AuthDto.LoginResponse(null, "Invalid credentials"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        // In a stateless JWT implementation, logout is handled client-side by discarding the token
        // For a more sophisticated implementation, you could maintain a token blacklist
        return ResponseEntity.ok().build();
    }
}
