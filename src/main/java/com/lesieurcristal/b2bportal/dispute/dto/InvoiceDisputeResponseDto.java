package com.lesieurcristal.b2bportal.dispute.dto;

import com.lesieurcristal.b2bportal.entity.app.InvoiceDispute;
import com.lesieurcristal.b2bportal.storage.ObjectKeys;

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
        boolean hasAttachment,
        String attachmentFilename,
        InvoiceDispute.DisputeStatus status,
        String previousInvoiceStatus,
        String resolutionNote,
        OffsetDateTime resolvedAt,
        OffsetDateTime createdAt,
        Long userId
) {
    public static InvoiceDisputeResponseDto from(InvoiceDispute d) {
        boolean hasFile = d.getFilePath() != null
                && !d.getFilePath().isBlank()
                && !ObjectKeys.isLegacyFilesystemPath(d.getFilePath());
        return new InvoiceDisputeResponseDto(
                d.getId(),
                d.getInvoiceNumber(),
                d.getCustomer() != null ? d.getCustomer().getCustomerNumber() : null,
                d.getReason(),
                d.getDescription(),
                hasFile,
                hasFile ? ObjectKeys.downloadFilename("justificatif", d.getId(), d.getFilePath()) : null,
                d.getStatus(),
                d.getPreviousInvoiceStatus(),
                d.getResolutionNote(),
                d.getResolvedAt(),
                d.getCreatedAt(),
                d.getUser() != null ? d.getUser().getId() : null
        );
    }
}
