package com.lesieurcristal.b2bportal.dispute.controller;

import com.lesieurcristal.b2bportal.config.OpenApiConfig;
import com.lesieurcristal.b2bportal.dispute.dto.InvoiceDisputeResponseDto;
import com.lesieurcristal.b2bportal.dispute.dto.ResolveDisputeRequest;
import com.lesieurcristal.b2bportal.dispute.service.DisputeService;
import com.lesieurcristal.b2bportal.storage.FileDownloadResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Routes d'arbitrage des contestations sous l'espace admin
 * ({@code /api/admin/invoices/disputes}, conforme au PRD §4.2).
 */
@Tag(name = "Admin — Contestations de factures", description = "Routes admin pour l'arbitrage des contestations")
@RestController
@RequestMapping("/api/admin/invoices/disputes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDisputeController {

    private final DisputeService disputeService;

    @Operation(summary = "Liste de toutes les contestations",
            description = "Retourne PENDING, APPROVED et REJECTED, plus récentes d'abord. "
                    + "Le filtre de statut est appliqué côté interface admin.")
    @GetMapping
    public ResponseEntity<List<InvoiceDisputeResponseDto>> all() {
        return ResponseEntity.ok(disputeService.listAllForAdmin());
    }

    @Operation(summary = "Approuve une contestation",
            description = "Clôture la contestation PENDING et restaure le statut de paiement de la facture. "
                    + "La génération d'avoir n'est pas implémentée dans ce MVP.")
    @PutMapping("/{id}/approve")
    public ResponseEntity<InvoiceDisputeResponseDto> approve(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ResolveDisputeRequest body) {
        return ResponseEntity.ok(disputeService.approveDispute(id,
                body != null ? body.resolutionNote() : null));
    }

    @Operation(summary = "Rejette une contestation avec un motif")
    @PutMapping("/{id}/reject")
    public ResponseEntity<InvoiceDisputeResponseDto> reject(
            @PathVariable Long id,
            @Valid @RequestBody ResolveDisputeRequest body) {
        return ResponseEntity.ok(disputeService.rejectDispute(id, body.resolutionNote()));
    }

    @Operation(summary = "Télécharge le justificatif d'une contestation")
    @GetMapping("/{id}/file")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        DisputeService.AttachmentFile file = disputeService.downloadForAdmin(id);
        return FileDownloadResponses.attachment(file.content(), file.contentType(), file.filename());
    }
}