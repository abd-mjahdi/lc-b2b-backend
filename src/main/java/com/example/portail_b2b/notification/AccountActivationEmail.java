package com.example.portail_b2b.notification;

import java.time.Instant;

public record AccountActivationEmail(
        String recipientEmail,
        String recipientFirstName,
        String login,
        String activationUrl,
        Instant expiresAt
) {
}
