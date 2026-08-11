package com.lesieurcristal.b2bportal.claim.controller;

import com.lesieurcristal.b2bportal.claim.dto.CreateReclamationRequest;
import com.lesieurcristal.b2bportal.claim.dto.ReclamationResponseDto;
import com.lesieurcristal.b2bportal.claim.service.ReclamationService;
import com.lesieurcristal.b2bportal.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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

@Tag(name = "Réclamations", description = "Ouverture et suivi des réclamations client")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReclamationController {

    private final ReclamationService reclamationService;

    @Operation(summary = "[Client] Ouvre une nouvelle réclamation")
    @PreAuthorize("hasRole('CLIENT')")
    @PostMapping("/claims")
    public ResponseEntity<ReclamationResponseDto> create(@Valid @RequestBody CreateReclamationRequest dto) {
        return ResponseEntity.ok(reclamationService.create(dto));
    }

    @Operation(summary = "[Client] Liste mes réclamations")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/claims/mine")
    public ResponseEntity<List<ReclamationResponseDto>> mine() {
        return ResponseEntity.ok(reclamationService.listForCurrentUser());
    }

    @Operation(summary = "[Admin] Liste toutes les réclamations")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/claims")
    public ResponseEntity<List<ReclamationResponseDto>> all() {
        return ResponseEntity.ok(reclamationService.listAllForAdmin());
    }

    @Operation(summary = "[Admin] Met à jour le statut d'une réclamation")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/claims/{id}/status")
    public ResponseEntity<ReclamationResponseDto> updateStatus(
            @PathVariable Long id, @RequestParam String status) {
        return ResponseEntity.ok(reclamationService.updateStatus(id, status));
    }
}
