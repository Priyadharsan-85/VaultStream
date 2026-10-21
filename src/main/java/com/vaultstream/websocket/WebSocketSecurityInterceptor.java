package com.vaultstream.websocket;

import com.vaultstream.repository.AccountRepository;
import com.vaultstream.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WebSocketSecurityInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    private final AccountRepository accountRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (accessor.getCommand() == StompCommand.CONNECT) {
            authenticate(accessor);
        } else if (accessor.getCommand() == StompCommand.SUBSCRIBE) {
            authorizeSubscription(accessor);
        } else if (accessor.getCommand() == StompCommand.SEND) {
            throw new IllegalArgumentException("Client messages are not supported");
        }

        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String authorization = accessor.getFirstNativeHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new IllegalArgumentException("A bearer token is required to connect");
        }

        String token = authorization.substring(7);
        if (!jwtTokenProvider.validateToken(token)) {
            throw new IllegalArgumentException("Invalid bearer token");
        }

        String username = jwtTokenProvider.getUsernameFromToken(token);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        accessor.setUser(authentication);
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        Principal principal = accessor.getUser();
        String destination = accessor.getDestination();
        if (principal == null || destination == null) {
            throw new IllegalArgumentException("Authenticated account subscription required");
        }

        String accountIdText;
        if (destination.startsWith("/topic/transactions/")) {
            accountIdText = destination.substring("/topic/transactions/".length());
        } else if (destination.startsWith("/topic/accounts/") && destination.endsWith("/balance")) {
            accountIdText = destination.substring("/topic/accounts/".length(), destination.length() - "/balance".length());
        } else {
            throw new IllegalArgumentException("Subscription destination is not allowed");
        }

        final UUID accountId;
        try {
            accountId = UUID.fromString(accountIdText);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Subscription destination is invalid", exception);
        }

        if (!accountRepository.existsByIdAndOwnerUser_Username(accountId, principal.getName())) {
            throw new IllegalArgumentException("Account not found");
        }
    }
}
