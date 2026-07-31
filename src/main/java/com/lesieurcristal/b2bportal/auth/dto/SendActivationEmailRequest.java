package com.lesieurcristal.b2bportal.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SendActivationEmailRequest(
        @NotBlank(message = "Recipient email is required")
        @Email(message = "Invalid recipient email address")
        String recipientEmail
) {
}
