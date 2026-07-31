package com.lesieurcristal.b2bportal.auth.dto;

public record ActivateAccountResponse(
        String message,
        String accessToken,
        String tokenType,
        AuthUserResponse user
) {
}
