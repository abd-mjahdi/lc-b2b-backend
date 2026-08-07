package com.lesieurcristal.b2bportal.order.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OrderResponseDto(
        String orderNumber,
        LocalDate orderDate,
        String customerNumber,
        String customerOrderReference,
        String productCode,
        String productLabel,
        BigDecimal quantityOrdered,
        BigDecimal quantityShipped,
        String salesUnit,
        BigDecimal netAmount,
        String currency,
        String shipToCity,
        String shipToCountry,
        LocalDate requestedDeliveryDate,
        LocalDate plannedDeliveryDate,
        LocalDate goodsIssueDate,
        String invoiceNumber,
        LocalDate invoiceDate,
        String invoiceStatus,
        BigDecimal invoiceTotalAmount,
        LocalDate invoiceDueDate,
        String status
) {
}
