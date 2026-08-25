package com.lesieurcristal.b2bportal.claim.controller;

import com.lesieurcristal.b2bportal.claim.dto.CreateReclamationRequest;
import com.lesieurcristal.b2bportal.claim.dto.ReclamationResponseDto;
import com.lesieurcristal.b2bportal.claim.service.ReclamationService;
import com.lesieurcristal.b2bportal.config.OpenApiConfig;
import com.lesieurcristal.b2bportal.storage.FileDownloadResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Réclamations", description = "Ouverture et suivi des réclamations client")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
@org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
public class ReclamationController {

    private final ReclamationService reclamationService;

    @Operation(summary = "[Client] Ouvre une nouvelle réclamation",
            description = "multipart/form-data : lotNumber, description, fichier optionnel (PDF/JPEG/PNG, 5 Mo max).")
    @PreAuthorize("hasRole('CLIENT')")
    @PostMapping(value = "/claims", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReclamationResponseDto> create(
            @RequestParam @NotBlank @Size(max = 50) String lotNumber,
            @RequestParam @NotBlank @Size(max = 4000) String description,
            @RequestParam(required = false) MultipartFile file) {
        return ResponseEntity.ok(reclamationService.create(
                new CreateReclamationRequest(lotNumber, description), file));
    }

    @Operation(summary = "[Client] Liste mes réclamations")
    @PreAuthorize("hasRole('CLIENT')")
    @GetMapping("/claims/mine")
    public ResponseEntity<List<ReclamationResponseDto>> mine() {
        return ResponseEntity.ok(reclamationService.listForCurrentUser());
    }

    @Operation(summary = "[Client] Télécharge la pièce jointe d'une réclamation")
    @PreAuthorize("hasRole('CLIENT')")
    @GetMapping("/claims/mine/{id}/file")
    public ResponseEntity<byte[]> downloadMine(@PathVariable Long id) {
        ReclamationService.AttachmentFile file = reclamationService.downloadForCurrentUser(id);
        return FileDownloadResponses.attachment(file.content(), file.contentType(), file.filename());
    }

    @Operation(summary = "[Admin] Liste toutes les réclamations")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/claims")
    public ResponseEntity<List<ReclamationResponseDto>> all() {
        return ResponseEntity.ok(reclamationService.listAllForAdmin());
    }

    @Operation(summary = "[Admin] Télécharge la pièce jointe d'une réclamation")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/claims/{id}/file")
    public ResponseEntity<byte[]> downloadAdmin(@PathVariable Long id) {
        ReclamationService.AttachmentFile file = reclamationService.downloadForAdmin(id);
        return FileDownloadResponses.attachment(file.content(), file.contentType(), file.filename());
    }

    @Operation(summary = "[Admin] Met à jour le statut d'une réclamation")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/claims/{id}/status")
    public ResponseEntity<ReclamationResponseDto> updateStatus(
            @PathVariable Long id, @RequestParam String status) {
        return ResponseEntity.ok(reclamationService.updateStatus(id, status));
    }
}
