package com.lesieurcristal.b2bportal.notification;

import java.time.OffsetDateTime;

public record AccountActivationEmail(
        String recipientEmail,
        String recipientFirstName,
        String login,
        String activationUrl,
        OffsetDateTime expiresAt
) {
}
