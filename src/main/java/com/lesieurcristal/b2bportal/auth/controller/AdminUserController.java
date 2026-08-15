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
    private final com.lesieurcristal.b2bportal.order.service.OrderService orderService;

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

    @org.springframework.web.bind.annotation.GetMapping
    public java.util.List<com.lesieurcristal.b2bportal.auth.dto.AuthUserResponse> getUsers(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String customerNumber
    ) {
        return adminUserRegistrationService.getUsers(customerNumber);
    }

    @org.springframework.web.bind.annotation.PostMapping("/{userId}/deactivate")
    public com.lesieurcristal.b2bportal.auth.dto.AuthUserResponse deactivateUser(@PathVariable Long userId) {
        return adminUserRegistrationService.deactivateUser(userId);
    }

    @org.springframework.web.bind.annotation.PostMapping("/{userId}/activate")
    public com.lesieurcristal.b2bportal.auth.dto.AuthUserResponse activateUser(@PathVariable Long userId) {
        return adminUserRegistrationService.activateUser(userId);
    }

    @org.springframework.web.bind.annotation.GetMapping("/customers")
    public java.util.List<com.lesieurcristal.b2bportal.auth.dto.CustomerAdminResponse> getAllCustomers() {
        return adminUserRegistrationService.getAllCustomersWithStats();
    }

    @org.springframework.web.bind.annotation.GetMapping("/customers/{customerNumber}")
    public com.lesieurcristal.b2bportal.entity.erpmock.Customer getCustomer(@PathVariable String customerNumber) {
        return adminUserRegistrationService.getCustomer(customerNumber);
    }

    @org.springframework.web.bind.annotation.GetMapping("/customers/{customerNumber}/orders")
    public org.springframework.data.domain.Page<com.lesieurcristal.b2bportal.order.dto.OrderResponseDto> getCustomerOrders(
            @PathVariable String customerNumber,
            @org.springframework.web.bind.annotation.RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate startDate,
            @org.springframework.web.bind.annotation.RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate endDate,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String status,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String invoiceStatus,
            @org.springframework.data.web.PageableDefault(size = 10) org.springframework.data.domain.Pageable pageable) {
        
        return orderService.getOrdersForCustomer(
                customerNumber, startDate, endDate, status, invoiceStatus, pageable);
    }
}
