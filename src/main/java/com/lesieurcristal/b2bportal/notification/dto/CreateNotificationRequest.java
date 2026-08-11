package com.lesieurcristal.b2bportal.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateNotificationRequest(
        @NotBlank @Size(max = 150) String title,
        @NotBlank String message,
        @NotBlank @Size(max = 50) String type,
        @Size(max = 50) String relatedEntityType,
        @Size(max = 50) String relatedEntityId,
        @Size(max = 255) String targetUrl,
        /** Vide = broadcast à tous les admins. */
        Long recipientUserId
) {
}