package com.example.portail_b2b.auth.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInMs,
        AuthUserResponse user
) {
}
