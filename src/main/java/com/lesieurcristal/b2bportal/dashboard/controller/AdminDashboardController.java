package com.lesieurcristal.b2bportal.dashboard.controller;

import com.lesieurcristal.b2bportal.config.OpenApiConfig;
import com.lesieurcristal.b2bportal.dashboard.dto.AdminKpiDto;
import com.lesieurcristal.b2bportal.repository.InvoiceDisputeRepository;
import com.lesieurcristal.b2bportal.repository.ReclamationRepository;
import com.lesieurcristal.b2bportal.repository.SampleRequestRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tableau de bord admin (PRD §3.2.1) — KPIs et indicateurs clés.
 */
@Tag(name = "Admin — Dashboard", description = "Compteurs de performance pour le tableau de bord admin")
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final SampleRequestRepository sampleRequestRepository;
    private final InvoiceDisputeRepository invoiceDisputeRepository;
    private final ReclamationRepository reclamationRepository;

    @Operation(summary = "Compteurs clés : commandes en attente, échantillons à expédier, contestations, réclamations")
    @GetMapping("/kpis")
    public ResponseEntity<AdminKpiDto> kpis() {
        AdminKpiDto kpis = new AdminKpiDto(
                // "Commandes en attente" = commandes avec statut ERP 'confirmed' (à valider).
                // Pour le MVP, on simule via le nombre de commandes en statut 'confirmed' —
                // un calcul exact pourrait rejoindre erp_mock.orders + order_status.
                sampleRequestRepository.countByStatus("new"),            // échantillons à préparer
                invoiceDisputeRepository.countByStatus(
                        com.lesieurcristal.b2bportal.entity.app.InvoiceDispute.DisputeStatus.PENDING),
                reclamationRepository.countByStatus("new")
        );
        return ResponseEntity.ok(kpis);
    }
}