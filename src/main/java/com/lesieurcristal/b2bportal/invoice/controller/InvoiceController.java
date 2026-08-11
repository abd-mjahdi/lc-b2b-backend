package com.lesieurcristal.b2bportal.invoice.controller;

import com.lesieurcristal.b2bportal.config.OpenApiConfig;
import com.lesieurcristal.b2bportal.entity.erpmock.Invoice;
import com.lesieurcristal.b2bportal.repository.InvoiceRepository;
import com.lesieurcristal.b2bportal.security.AuthenticatedUser;
import com.lesieurcristal.b2bportal.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Endpoints de consultation des factures (PRD §4.2).
 * Les contestations sont gérées dans {@link com.lesieurcristal.b2bportal.dispute.controller.DisputeController}.
 */
@Tag(name = "Factures client", description = "Consultation des factures du client connecté")
@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class InvoiceController {

    private final InvoiceRepository invoiceRepository;

    @Operation(summary = "Liste des factures du client connecté")
    @GetMapping
    public ResponseEntity<List<InvoiceDto>> listMyInvoices() {
        AuthenticatedUser current = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Non authentifié"));
        if (current.getCustomerNumber() == null) {
            // Admin : on ne montre pas de factures par défaut ici (route admin dédiée)
            return ResponseEntity.ok(List.of());
        }
        List<InvoiceDto> result = invoiceRepository
                .findByCustomer_CustomerNumberOrderByInvoiceDateDesc(current.getCustomerNumber())
                .stream()
                .map(InvoiceDto::from)
                .toList();
        return ResponseEntity.ok(result);
    }

    public record InvoiceDto(
            String invoiceNumber,
            String orderNumber,
            LocalDate invoiceDate,
            LocalDate dueDate,
            LocalDate paymentDate,
            BigDecimal netAmount,
            BigDecimal vatAmount,
            BigDecimal totalAmount,
            String currency,
            String invoiceStatus
    ) {
        public static InvoiceDto from(Invoice i) {
            return new InvoiceDto(
                    i.getInvoiceNumber(),
                    i.getOrder() != null ? i.getOrder().getOrderNumber() : null,
                    i.getInvoiceDate(),
                    i.getDueDate(),
                    i.getPaymentDate(),
                    i.getNetAmount(),
                    i.getVatAmount(),
                    i.getTotalAmount(),
                    i.getCurrency(),
                    i.getInvoiceStatus()
            );
        }
    }
}