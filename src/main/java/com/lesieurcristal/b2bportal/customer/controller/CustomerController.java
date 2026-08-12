package com.lesieurcristal.b2bportal.customer.controller;

import com.lesieurcristal.b2bportal.config.OpenApiConfig;
import com.lesieurcristal.b2bportal.customer.dto.CustomerProfileResponse;
import com.lesieurcristal.b2bportal.customer.dto.UpdateCustomerProfileRequest;
import com.lesieurcristal.b2bportal.customer.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Profil client", description = "Consultation et mise à jour de la fiche entreprise")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@PreAuthorize("isAuthenticated()")
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @Operation(summary = "Profil du client connecté",
            description = "Retourne la fiche erp_mock.customers et le journal d'audit app.client_change_log.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profil récupéré"),
            @ApiResponse(responseCode = "400", description = "Aucun numéro client associé"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "404", description = "Client introuvable")
    })
    @PreAuthorize("hasRole('CLIENT')")
    @GetMapping("/me")
    public CustomerProfileResponse getMyProfile() {
        return customerService.getMyProfile();
    }

    @Operation(summary = "Mettre à jour mon profil",
            description = "Met à jour les champs autorisés (phone, email) et journalise chaque changement "
                    + "dans app.client_change_log. Les données SAP (raison sociale, TVA, adresse…) restent verrouillées.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profil mis à jour"),
            @ApiResponse(responseCode = "400", description = "Données invalides ou aucun client associé"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "404", description = "Client introuvable")
    })
    @PreAuthorize("hasRole('CLIENT')")
    @PutMapping("/me")
    public CustomerProfileResponse updateMyProfile(@Valid @RequestBody UpdateCustomerProfileRequest request) {
        return customerService.updateMyProfile(request);
    }
}
