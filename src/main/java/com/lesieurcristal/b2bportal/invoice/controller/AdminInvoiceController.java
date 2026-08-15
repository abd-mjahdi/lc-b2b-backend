package com.lesieurcristal.b2bportal.invoice.controller;

import com.lesieurcristal.b2bportal.config.OpenApiConfig;
import com.lesieurcristal.b2bportal.invoice.controller.InvoiceController.InvoiceDetailDto;
import com.lesieurcristal.b2bportal.invoice.controller.InvoiceController.InvoiceDto;
import com.lesieurcristal.b2bportal.invoice.service.InvoiceDocumentService;
import com.lesieurcristal.b2bportal.repository.InvoiceRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Admin — Factures", description = "Consultation et téléchargement PDF des factures client")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminInvoiceController {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceDocumentService invoiceDocumentService;

    @Operation(summary = "Liste des factures d'un client")
    @GetMapping("/customers/{customerNumber}/invoices")
    public ResponseEntity<List<InvoiceDto>> listForCustomer(@PathVariable String customerNumber) {
        List<InvoiceDto> result = invoiceRepository
                .findByCustomer_CustomerNumberWithOrderOrderByInvoiceDateDesc(customerNumber)
                .stream()
                .map(InvoiceDto::from)
                .toList();
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Détail d'une facture (admin)")
    @GetMapping("/invoices/{invoiceNumber}")
    public ResponseEntity<InvoiceDetailDto> getInvoice(@PathVariable String invoiceNumber) {
        return ResponseEntity.ok(InvoiceDetailDto.from(
                invoiceRepository.findByInvoiceNumberWithOrder(invoiceNumber)
                        .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                                "Facture introuvable : " + invoiceNumber))));
    }

    @Operation(summary = "Télécharger le PDF d'une facture (admin)")
    @GetMapping("/invoices/{invoiceNumber}/file")
    public ResponseEntity<byte[]> downloadInvoiceFile(@PathVariable String invoiceNumber) {
        InvoiceDocumentService.InvoiceFile file = invoiceDocumentService.downloadInvoicePdf(invoiceNumber);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.filename() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(file.content().length)
                .body(file.content());
    }
}
