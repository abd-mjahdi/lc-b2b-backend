package com.lesieurcristal.b2bportal.dispute.dto;

import com.lesieurcristal.b2bportal.entity.app.InvoiceDispute;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

/**
 * Payload pour qu'un client dépose une contestation sur une facture.
 */
public record InvoiceDisputeResponseDto(
        Long id,
        String invoiceNumber,
        String customerNumber,
        InvoiceDispute.DisputeReason reason,
        String description,
        String filePath,
        InvoiceDispute.DisputeStatus status,
        String previousInvoiceStatus,
        String resolutionNote,
        OffsetDateTime resolvedAt,
        OffsetDateTime createdAt,
        Long userId
) {
    public static InvoiceDisputeResponseDto from(InvoiceDispute d) {
        return new InvoiceDisputeResponseDto(
                d.getId(),
                d.getInvoiceNumber(),
                d.getCustomer() != null ? d.getCustomer().getCustomerNumber() : null,
                d.getReason(),
                d.getDescription(),
                d.getFilePath(),
                d.getStatus(),
                d.getPreviousInvoiceStatus(),
                d.getResolutionNote(),
                d.getResolvedAt(),
                d.getCreatedAt(),
                d.getUser() != null ? d.getUser().getId() : null
        );
    }
}
