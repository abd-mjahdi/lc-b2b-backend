package com.lesieurcristal.b2bportal.auth.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInMs,
        AuthUserResponse user
) {
}
