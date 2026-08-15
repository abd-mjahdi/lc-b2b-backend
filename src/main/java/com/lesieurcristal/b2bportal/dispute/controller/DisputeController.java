package com.lesieurcristal.b2bportal.dispute.controller;

import com.lesieurcristal.b2bportal.config.OpenApiConfig;
import com.lesieurcristal.b2bportal.dispute.dto.CreateInvoiceDisputeRequest;
import com.lesieurcristal.b2bportal.dispute.dto.InvoiceDisputeResponseDto;
import com.lesieurcristal.b2bportal.dispute.dto.ResolveDisputeRequest;
import com.lesieurcristal.b2bportal.dispute.service.DisputeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Factures & Contestations", description = "Endpoints client et admin pour la gestion des factures et des litiges")
@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
@org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
public class DisputeController {

    private final DisputeService disputeService;

    // -------- Espace client --------

    @Operation(summary = "[Client] Déclare une contestation sur une facture")
    @PreAuthorize("hasRole('CLIENT')")
    @PostMapping("/{invoiceNumber}/dispute")
    public ResponseEntity<InvoiceDisputeResponseDto> declareDispute(
            @PathVariable String invoiceNumber,
            @RequestParam(required = false) String filePath,
            @Valid @RequestBody CreateInvoiceDisputeRequest dto) {
        return ResponseEntity.ok(disputeService.createDispute(invoiceNumber, dto, filePath));
    }

    @Operation(summary = "[Client] Liste ses propres contestations")
    @PreAuthorize("hasRole('CLIENT')")
    @GetMapping("/disputes/mine")
    public ResponseEntity<List<InvoiceDisputeResponseDto>> myDisputes() {
        return ResponseEntity.ok(disputeService.listForCurrentUser());
    }

    @Operation(summary = "[Client] Détail d'une de ses contestations")
    @PreAuthorize("hasRole('CLIENT')")
    @GetMapping("/disputes/mine/{id}")
    public ResponseEntity<InvoiceDisputeResponseDto> getDispute(@PathVariable Long id) {
        return ResponseEntity.ok(disputeService.getByIdForCurrentUser(id));
    }

    // -------- Espace admin --------

    @Operation(summary = "[Admin] Liste des contestations en attente d'arbitrage")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/disputes/pending")
    public ResponseEntity<List<InvoiceDisputeResponseDto>> pendingDisputes() {
        return ResponseEntity.ok(disputeService.listPendingForAdmin());
    }

    @Operation(summary = "[Admin] Liste de toutes les contestations")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/disputes/all")
    public ResponseEntity<List<InvoiceDisputeResponseDto>> allDisputes() {
        return ResponseEntity.ok(disputeService.listAllForAdmin());
    }

    @Operation(summary = "[Admin] Approuve une contestation",
            description = "Clôture la contestation PENDING et restaure le statut de paiement de la facture. "
                    + "La génération d'avoir n'est pas implémentée dans ce MVP.")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/disputes/{id}/approve")
    public ResponseEntity<InvoiceDisputeResponseDto> approve(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ResolveDisputeRequest body) {
        return ResponseEntity.ok(disputeService.approveDispute(id,
                body != null ? body.resolutionNote() : null));
    }

    @Operation(summary = "[Admin] Rejette une contestation avec motif")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/disputes/{id}/reject")
    public ResponseEntity<InvoiceDisputeResponseDto> reject(
            @PathVariable Long id,
            @Valid @RequestBody ResolveDisputeRequest body) {
        return ResponseEntity.ok(disputeService.rejectDispute(id, body.resolutionNote()));
    }
}