package com.vaultstream.auth;

import lombok.*;

@Getter
@Setter
public class AuthDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {
        @NonNull
        private String username;
        @NonNull
        private String password;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LoginResponse {
        private String token;
        private String errorMessage;
    }
}