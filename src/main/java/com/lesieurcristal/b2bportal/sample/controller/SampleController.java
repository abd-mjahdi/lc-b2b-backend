package com.lesieurcristal.b2bportal.sample.controller;

import com.lesieurcristal.b2bportal.config.OpenApiConfig;
import com.lesieurcristal.b2bportal.sample.dto.CreateSampleRequestDto;
import com.lesieurcristal.b2bportal.sample.dto.LinkSampleToOrderDto;
import com.lesieurcristal.b2bportal.sample.dto.SampleRequestResponseDto;
import com.lesieurcristal.b2bportal.sample.dto.UpdateSampleStatusDto;
import com.lesieurcristal.b2bportal.sample.service.SampleService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Échantillons", description = "Gestion des demandes d'échantillons (PRD §2.1, §4.2)")
@org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
@RestController
@RequestMapping("/api/samples")
@RequiredArgsConstructor
public class SampleController {

    private final SampleService sampleService;

    @Operation(summary = "Crée une demande d'échantillon isolée")
    @PostMapping
    public ResponseEntity<SampleRequestResponseDto> create(
            @Valid @RequestBody CreateSampleRequestDto dto) {
        return ResponseEntity.ok(sampleService.createSampleRequest(dto));
    }

    @Operation(summary = "Liste des demandes d'échantillons (admin = tout, client = siennes)")
    @GetMapping
    public ResponseEntity<List<SampleRequestResponseDto>> list() {
        return ResponseEntity.ok(sampleService.listSamplesForCurrentUser());
    }

    @Operation(summary = "Détail d'une demande")
    @GetMapping("/{id}")
    public ResponseEntity<SampleRequestResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(sampleService.getById(id));
    }

    @Operation(summary = "[Admin] Met à jour le statut logistique d'un échantillon",
            description = "Notifie automatiquement le client en cas de changement d'état.")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/status")
    public ResponseEntity<SampleRequestResponseDto> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSampleStatusDto dto) {
        return ResponseEntity.ok(sampleService.updateStatus(id, dto.status()));
    }

    @Operation(summary = "[Admin] Lie l'échantillon à une commande ERP finale (boucle de conversion)")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/link-order")
    public ResponseEntity<SampleRequestResponseDto> linkToOrder(
            @PathVariable Long id,
            @Valid @RequestBody LinkSampleToOrderDto dto) {
        return ResponseEntity.ok(sampleService.linkToOrder(id, dto.orderNumber()));
    }
}
