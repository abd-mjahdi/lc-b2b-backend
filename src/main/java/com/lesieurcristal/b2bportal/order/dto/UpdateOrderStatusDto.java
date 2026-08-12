package com.lesieurcristal.b2bportal.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

/**
 * Payload admin pour mettre à jour {@code erp_mock.order_status}.
 */
public record UpdateOrderStatusDto(
        @NotBlank
        @Pattern(
                regexp = "confirmed|in_preparation|shipped|delivered|cancelled",
                message = "Statut autorisé : confirmed, in_preparation, shipped, delivered, cancelled"
        )
        String status,
        LocalDate expectedDeliveryDate,
        String carrierName,
        String carrierReference
) {
}
