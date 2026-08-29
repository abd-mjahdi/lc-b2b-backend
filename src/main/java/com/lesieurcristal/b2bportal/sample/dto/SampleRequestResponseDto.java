package com.lesieurcristal.b2bportal.sample.dto;

import com.lesieurcristal.b2bportal.entity.app.SampleRequest;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record SampleRequestResponseDto(
        Long id,
        String productCode,
        String productName,
        BigDecimal quantity,
        String contactName,
        String contactAddress,
        String status,
        String linkedOrderNumber,
        String resultingOrderNumber,
        String customerNumber,
        String customerName,
        Long userId,
        OffsetDateTime requestedAt
) {
    public static SampleRequestResponseDto from(SampleRequest sr) {
        return new SampleRequestResponseDto(
                sr.getId(),
                sr.getProduct() != null ? sr.getProduct().getCode() : null,
                sr.getProduct() != null ? sr.getProduct().getName() : null,
                sr.getQuantity(),
                sr.getContactName(),
                sr.getContactAddress(),
                sr.getStatus(),
                sr.getLinkedOrderNumber(),
                sr.getResultingOrder() != null ? sr.getResultingOrder().getOrderNumber() : null,
                sr.getCustomer() != null ? sr.getCustomer().getCustomerNumber() : null,
                sr.getCustomer() != null ? sr.getCustomer().getCompanyName() : null,
                sr.getUser() != null ? sr.getUser().getId() : null,
                sr.getRequestedAt()
        );
    }
}