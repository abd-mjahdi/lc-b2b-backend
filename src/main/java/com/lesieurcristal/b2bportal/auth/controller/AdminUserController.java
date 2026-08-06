package com.lesieurcristal.b2bportal.auth.controller;

import com.lesieurcristal.b2bportal.auth.dto.AdminRegisterClientRequest;
import com.lesieurcristal.b2bportal.auth.dto.AdminRegisterClientResponse;
import com.lesieurcristal.b2bportal.auth.service.AdminUserRegistrationService;
import com.lesieurcristal.b2bportal.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.lesieurcristal.b2bportal.auth.dto.SendActivationEmailRequest;
import com.lesieurcristal.b2bportal.auth.dto.SendActivationEmailResponse;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Administration Utilisateurs", description = "Endpoints réservés aux administrateurs pour la gestion des comptes")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserRegistrationService adminUserRegistrationService;

    @Operation(summary = "Création d'un compte client par l'administrateur", description = "Crée un compte client associé à un numéro client SAP et génère un jeton d'activation.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Compte créé et jeton d'activation généré"),
            @ApiResponse(responseCode = "400", description = "Données invalides ou numéro client inexistant"),
            @ApiResponse(responseCode = "403", description = "Accès réservé aux administrateurs")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminRegisterClientResponse registerClient(@Valid @RequestBody AdminRegisterClientRequest request) {
        return adminUserRegistrationService.registerClient(request);
    }

    @PostMapping("/{userId}/send-activation")
    public SendActivationEmailResponse sendActivationEmail(
            @PathVariable Long userId,
            @RequestBody(required = false) SendActivationEmailRequest request
    ) {
        return adminUserRegistrationService.sendActivationEmail(userId, request);
    }
}
