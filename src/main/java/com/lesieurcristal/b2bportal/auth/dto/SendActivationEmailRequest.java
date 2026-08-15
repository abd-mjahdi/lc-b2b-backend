package com.lesieurcristal.b2bportal.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Optional body for resending activation mail.
 * If {@code recipientEmail} is set, it must match the account email — arbitrary
 * destinations are rejected to prevent activation-link exfiltration.
 */
public record SendActivationEmailRequest(
        @Email(message = "Invalid recipient email address")
        @Size(max = 150)
        String recipientEmail
) {
}
