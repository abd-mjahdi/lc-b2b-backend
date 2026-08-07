package com.lesieurcristal.b2bportal.order.controller;

import com.lesieurcristal.b2bportal.order.dto.OrderResponseDto;
import com.lesieurcristal.b2bportal.order.dto.OrderStatusResponseDto;
import com.lesieurcristal.b2bportal.order.service.OrderService;
import com.lesieurcristal.b2bportal.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Commandes", description = "Endpoints de consultation des commandes et de leur statut en direct")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @Operation(summary = "Historique des commandes du client connecté", description = "Récupère la liste de toutes les commandes du client authentifié avec le statut de la facture associée.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste des commandes récupérée avec succès"),
            @ApiResponse(responseCode = "401", description = "Utilisateur non authentifié"),
            @ApiResponse(responseCode = "400", description = "Aucun numéro client associé")
    })
    @GetMapping
    public ResponseEntity<org.springframework.data.domain.Page<OrderResponseDto>> getOrders(
            @org.springframework.web.bind.annotation.RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate startDate,
            @org.springframework.web.bind.annotation.RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate endDate,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String status,
            @org.springframework.data.web.PageableDefault(size = 10) org.springframework.data.domain.Pageable pageable) {
        
        org.springframework.data.domain.Page<OrderResponseDto> orders = orderService.getOrdersForCurrentUser(startDate, endDate, status, pageable);
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Statut en direct d'une commande", description = "Récupère le statut en direct (transporteur, livraison prévue, statut courant) d'une commande spécifiée par son numéro.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statut de la commande récupéré avec succès"),
            @ApiResponse(responseCode = "401", description = "Utilisateur non authentifié"),
            @ApiResponse(responseCode = "404", description = "Commande introuvable ou n'appartenant pas au client")
    })
    @GetMapping("/{id}/status")
    public ResponseEntity<OrderStatusResponseDto> getOrderStatus(
            @Parameter(description = "Numéro de la commande SAP (ex: 4500010001)", required = true)
            @PathVariable("id") String orderNumber) {
        OrderStatusResponseDto status = orderService.getOrderStatus(orderNumber);
        return ResponseEntity.ok(status);
    }
}
