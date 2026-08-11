package com.lesieurcristal.b2bportal.dispute.dto;

import com.lesieurcristal.b2bportal.entity.app.InvoiceDispute;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload pour qu'un client dépose une contestation sur une facture.
 */
public record CreateInvoiceDisputeRequest(
        @NotNull InvoiceDispute.DisputeReason reason,
        @NotBlank @Size(max = 4000) String description
        // Le file_path (justificatif) est ajouté séparément via l'endpoint d'upload
) {
}