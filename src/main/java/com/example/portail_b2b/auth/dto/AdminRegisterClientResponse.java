package com.example.portail_b2b.auth.dto;

import java.time.Instant;

public record AdminRegisterClientResponse(
        Long userId,
        String login,
        String email,
        String customerNumber,
        boolean active,
        Instant activationEmailSentAt,
        String message
) {
}
