package com.lesieurcristal.b2bportal.auth.dto;

import java.time.OffsetDateTime;

public record AdminRegisterClientResponse(
        Long userId,
        String login,
        String email,
        String customerNumber,
        boolean active,
        OffsetDateTime activationEmailSentAt,
        String message
) {
}
