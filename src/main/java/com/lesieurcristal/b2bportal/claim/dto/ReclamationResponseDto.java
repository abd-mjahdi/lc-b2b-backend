package com.lesieurcristal.b2bportal.claim.dto;

import com.lesieurcristal.b2bportal.entity.app.Reclamation;
import com.lesieurcristal.b2bportal.storage.ObjectKeys;

import java.time.OffsetDateTime;

public record ReclamationResponseDto(
        Long id,
        String customerNumber,
        String customerName,
        String userName,
        String lotNumber,
        String description,
        boolean hasAttachment,
        String attachmentFilename,
        String status,
        OffsetDateTime receivedAt
) {
    public static ReclamationResponseDto from(Reclamation r) {
        boolean hasFile = ObjectKeys.isStoredObjectKey(r.getAttachmentPath());
        return new ReclamationResponseDto(
                r.getId(),
                r.getCustomer() != null ? r.getCustomer().getCustomerNumber() : null,
                r.getCustomer() != null ? r.getCustomer().getCompanyName() : null,
                formatUserName(r),
                r.getLotNumber(),
                r.getDescription(),
                hasFile,
                hasFile ? ObjectKeys.downloadFilename("reclamation", r.getId(), r.getAttachmentPath()) : null,
                r.getStatus(),
                r.getReceivedAt()
        );
    }

    private static String formatUserName(Reclamation r) {
        if (r.getUser() == null) {
            return null;
        }
        String first = r.getUser().getFirstName() != null ? r.getUser().getFirstName() : "";
        String last = r.getUser().getLastName() != null ? r.getUser().getLastName() : "";
        String combined = (first + " " + last).trim();
        return combined.isEmpty() ? r.getUser().getLogin() : combined;
    }
}
