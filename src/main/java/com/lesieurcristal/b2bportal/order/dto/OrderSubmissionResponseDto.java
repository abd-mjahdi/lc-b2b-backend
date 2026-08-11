package com.lesieurcristal.b2bportal.order.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Réponse renvoyée au client après la création d'une commande.
 */
public record OrderSubmissionResponseDto(
        String orderNumber,
        String customerOrderReference,
        LocalDate orderDate,
        LocalDate requestedDeliveryDate,
        BigDecimal totalNetAmount,
        String currency,
        String shipToCity,
        String shipToCountry,
        String transportMethod,
        String status,
        int quantityOrdered,
        List<String> linkedSampleIds,
        OffsetDateTime createdAt
) {
}