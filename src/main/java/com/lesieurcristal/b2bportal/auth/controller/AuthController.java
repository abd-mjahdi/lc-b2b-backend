package com.lesieurcristal.b2bportal.auth.controller;

import com.lesieurcristal.b2bportal.auth.dto.ActivateAccountRequest;
import com.lesieurcristal.b2bportal.auth.dto.ActivateAccountResponse;
import com.lesieurcristal.b2bportal.auth.dto.LoginRequest;
import com.lesieurcristal.b2bportal.auth.dto.LoginResponse;
import com.lesieurcristal.b2bportal.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentification", description = "Endpoints de connexion et d'activation de compte")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Connexion utilisateur", description = "Authentifie un utilisateur via login et mot de passe et retourne un jeton JWT.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Connexion réussie, jeton JWT retourné"),
            @ApiResponse(responseCode = "401", description = "Identifiants invalides ou compte inactif")
    })
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @Operation(summary = "Activation de compte", description = "Active un compte utilisateur à l'aide d'un jeton d'activation et définit le mot de passe initial.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Compte activé avec succès"),
            @ApiResponse(responseCode = "400", description = "Jeton d'activation invalide ou expiré")
    })
    @PostMapping("/activate")
    @ResponseStatus(HttpStatus.OK)
    public ActivateAccountResponse activate(@Valid @RequestBody ActivateAccountRequest request) {
        return authService.activateAccount(request);
    }
}
