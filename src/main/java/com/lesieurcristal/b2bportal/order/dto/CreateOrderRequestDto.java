package com.lesieurcristal.b2bportal.order.dto;

import com.lesieurcristal.b2bportal.order.dto.CreateOrderRequestDto.OrderLine;
import com.lesieurcristal.b2bportal.order.dto.CreateOrderRequestDto.SampleLine;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Payload pour la soumission d'une commande (PRD §4.2 /api/orders).
 * Supporte la liaison d'échantillons groupés via {@link #sampleProductCodes}.
 */
public record CreateOrderRequestDto(
        @NotBlank String customerOrderReference,
        @NotBlank String shipToCity,
        @NotBlank String shipToCountry,
        LocalDate requestedDeliveryDate,
        @NotBlank String transportMethod,
        @NotEmpty @Valid List<OrderLine> orderLines,
        /** Codes produits à joindre comme échantillons dans la même livraison. */
        List<SampleLine> sampleProductCodes
) {
    public record OrderLine(
            @NotBlank String productCode,
            @NotNull @Positive BigDecimal quantity,
            String salesUnit
    ) {}

    public record SampleLine(
            @NotBlank String productCode,
            @NotNull @Positive BigDecimal quantity
    ) {}
}
