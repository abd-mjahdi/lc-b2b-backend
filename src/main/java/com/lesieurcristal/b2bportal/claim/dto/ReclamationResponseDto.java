package com.lesieurcristal.b2bportal.claim.dto;

import com.lesieurcristal.b2bportal.entity.app.Reclamation;

import java.time.OffsetDateTime;

public record ReclamationResponseDto(
        Long id,
        String customerNumber,
        String lotNumber,
        String description,
        String attachmentPath,
        String status,
        OffsetDateTime receivedAt
) {
    public static ReclamationResponseDto from(Reclamation r) {
        return new ReclamationResponseDto(
                r.getId(),
                r.getCustomer() != null ? r.getCustomer().getCustomerNumber() : null,
                r.getLotNumber(),
                r.getDescription(),
                r.getAttachmentPath(),
                r.getStatus(),
                r.getReceivedAt()
        );
    }
}
