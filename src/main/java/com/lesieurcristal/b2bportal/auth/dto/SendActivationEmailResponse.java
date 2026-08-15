package com.lesieurcristal.b2bportal.auth.dto;

import java.time.OffsetDateTime;

/**
 * Confirmation of activation email send. Intentionally omits the activation URL
 * so the raw token never leaves the mail channel.
 */
public record SendActivationEmailResponse(
        Long userId,
        String recipientEmail,
        OffsetDateTime sentAt,
        String message
) {
}
