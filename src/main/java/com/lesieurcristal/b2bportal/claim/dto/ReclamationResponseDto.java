package com.lesieurcristal.b2bportal.claim.dto;

import com.lesieurcristal.b2bportal.entity.app.Reclamation;
import com.lesieurcristal.b2bportal.storage.ObjectKeys;

import java.time.OffsetDateTime;

public record ReclamationResponseDto(
        Long id,
        String customerNumber,
        String lotNumber,
        String description,
        boolean hasAttachment,
        String attachmentFilename,
        String status,
        OffsetDateTime receivedAt
) {
    public static ReclamationResponseDto from(Reclamation r) {
        boolean hasFile = r.getAttachmentPath() != null
                && !r.getAttachmentPath().isBlank()
                && !ObjectKeys.isLegacyFilesystemPath(r.getAttachmentPath())
                && !r.getAttachmentPath().startsWith("/uploads/");
        return new ReclamationResponseDto(
                r.getId(),
                r.getCustomer() != null ? r.getCustomer().getCustomerNumber() : null,
                r.getLotNumber(),
                r.getDescription(),
                hasFile,
                hasFile ? ObjectKeys.downloadFilename("reclamation", r.getId(), r.getAttachmentPath()) : null,
                r.getStatus(),
                r.getReceivedAt()
        );
    }
}
