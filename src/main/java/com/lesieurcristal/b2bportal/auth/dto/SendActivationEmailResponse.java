package com.lesieurcristal.b2bportal.auth.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SendActivationEmailResponse(
        Long userId,
        String recipientEmail,
        String activationUrl,
        OffsetDateTime sentAt,
        String message
) {
}
