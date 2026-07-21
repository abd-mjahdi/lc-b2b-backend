package com.example.portail_b2b.auth.dto;

public record ActivateAccountResponse(
        String message,
        AuthUserResponse user
) {
}
