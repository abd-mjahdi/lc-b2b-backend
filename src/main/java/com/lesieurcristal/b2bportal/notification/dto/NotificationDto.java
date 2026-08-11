package com.lesieurcristal.b2bportal.notification.dto;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * Forme JSON transmise via SSE et via l'endpoint REST
 * {@code /api/notifications}. Doit rester {@link Serializable} pour
 * pouvoir être mise en cache si besoin.
 */
public record NotificationDto(
        Long id,
        String recipientRole,
        Long recipientUserId,
        String title,
        String message,
        String type,
        String relatedEntityType,
        String relatedEntityId,
        String targetUrl,
        boolean isRead,
        OffsetDateTime createdAt
) implements Serializable {
}