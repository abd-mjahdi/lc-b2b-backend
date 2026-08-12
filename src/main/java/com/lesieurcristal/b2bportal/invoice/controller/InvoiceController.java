package com.lesieurcristal.b2bportal.invoice.controller;

import com.lesieurcristal.b2bportal.config.OpenApiConfig;
import com.lesieurcristal.b2bportal.entity.erpmock.Invoice;
import com.lesieurcristal.b2bportal.entity.erpmock.Order;
import com.lesieurcristal.b2bportal.repository.InvoiceRepository;
import com.lesieurcristal.b2bportal.security.AuthenticatedUser;
import com.lesieurcristal.b2bportal.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Endpoints de consultation des factures (PRD §4.2).
 * Les contestations sont gérées dans {@link com.lesieurcristal.b2bportal.dispute.controller.DisputeController}.
 */
@Tag(name = "Factures client", description = "Consultation des factures du client connecté")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
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

    @Operation(summary = "Détail d'une facture",
            description = "Retourne une facture avec le lien commande associée. "
                    + "Isolée au client connecté (404 si hors périmètre).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Facture trouvée"),
            @ApiResponse(responseCode = "400", description = "Aucun numéro client associé"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "404", description = "Facture introuvable ou hors périmètre")
    })
    @GetMapping("/{invoiceNumber}")
    public ResponseEntity<InvoiceDetailDto> getInvoice(
            @Parameter(description = "Numéro de facture SAP (ex: 900010001)", required = true)
            @PathVariable String invoiceNumber) {

        AuthenticatedUser current = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Non authentifié"));
        if (current.getCustomerNumber() == null || current.getCustomerNumber().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucun numéro client associé");
        }

        Invoice invoice = invoiceRepository.findByInvoiceNumberWithOrder(invoiceNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Facture introuvable"));

        if (invoice.getCustomer() == null
                || !current.getCustomerNumber().equals(invoice.getCustomer().getCustomerNumber())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Facture introuvable");
        }

        return ResponseEntity.ok(InvoiceDetailDto.from(invoice));
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

    public record InvoiceDetailDto(
            String invoiceNumber,
            LocalDate invoiceDate,
            LocalDate dueDate,
            LocalDate paymentDate,
            BigDecimal netAmount,
            BigDecimal vatAmount,
            BigDecimal totalAmount,
            String currency,
            String invoiceStatus,
            String customerNumber,
            OrderLinkDto order
    ) {
        public static InvoiceDetailDto from(Invoice i) {
            return new InvoiceDetailDto(
                    i.getInvoiceNumber(),
                    i.getInvoiceDate(),
                    i.getDueDate(),
                    i.getPaymentDate(),
                    i.getNetAmount(),
                    i.getVatAmount(),
                    i.getTotalAmount(),
                    i.getCurrency(),
                    i.getInvoiceStatus(),
                    i.getCustomer() != null ? i.getCustomer().getCustomerNumber() : null,
                    OrderLinkDto.from(i.getOrder())
            );
        }
    }

    public record OrderLinkDto(
            String orderNumber,
            LocalDate orderDate,
            String customerOrderReference,
            String productCode,
            String productLabel,
            BigDecimal quantityOrdered,
            BigDecimal netAmount,
            String currency
    ) {
        public static OrderLinkDto from(Order order) {
            if (order == null) {
                return null;
            }
            return new OrderLinkDto(
                    order.getOrderNumber(),
                    order.getOrderDate(),
                    order.getCustomerOrderReference(),
                    order.getProductCode(),
                    order.getProductLabel(),
                    order.getQuantityOrdered(),
                    order.getNetAmount(),
                    order.getCurrency()
            );
        }
    }
}
