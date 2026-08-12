package com.lesieurcristal.b2bportal.order.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Détail complet d'une commande : lignes produit, résumé facture, statut live.
 */
public record OrderDetailDto(
        String orderNumber,
        LocalDate orderDate,
        String customerNumber,
        String customerOrderReference,
        String shipToCity,
        String shipToCountry,
        LocalDate requestedDeliveryDate,
        LocalDate plannedDeliveryDate,
        LocalDate goodsIssueDate,
        BigDecimal totalNetAmount,
        String currency,
        List<OrderLineDetailDto> lines,
        InvoiceSummaryDto invoice,
        OrderStatusResponseDto status
) {
    public record OrderLineDetailDto(
            String orderNumber,
            String productCode,
            String productLabel,
            BigDecimal quantityOrdered,
            BigDecimal quantityShipped,
            String salesUnit,
            BigDecimal netAmount,
            String currency
    ) {
    }

    public record InvoiceSummaryDto(
            String invoiceNumber,
            LocalDate invoiceDate,
            String invoiceStatus,
            BigDecimal netAmount,
            BigDecimal vatAmount,
            BigDecimal totalAmount,
            String currency,
            LocalDate dueDate,
            LocalDate paymentDate
    ) {
    }
}
