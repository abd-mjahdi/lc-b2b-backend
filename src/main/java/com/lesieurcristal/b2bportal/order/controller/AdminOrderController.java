package com.lesieurcristal.b2bportal.order.controller;

import com.lesieurcristal.b2bportal.config.OpenApiConfig;
import com.lesieurcristal.b2bportal.order.dto.OrderStatusResponseDto;
import com.lesieurcristal.b2bportal.order.dto.UpdateOrderStatusDto;
import com.lesieurcristal.b2bportal.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Commandes (Admin)", description = "Mise à jour des statuts logistiques des commandes")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @Operation(summary = "Met à jour le statut live d'une commande",
            description = "Met à jour erp_mock.order_status et notifie le client via PortalNotificationService.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statut mis à jour"),
            @ApiResponse(responseCode = "400", description = "Statut invalide"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "403", description = "Accès admin requis"),
            @ApiResponse(responseCode = "404", description = "Commande introuvable")
    })
    @PutMapping("/{orderNumber}/status")
    public ResponseEntity<OrderStatusResponseDto> updateOrderStatus(
            @Parameter(description = "Numéro de la commande SAP (ex: 4500010001)", required = true)
            @PathVariable String orderNumber,
            @Valid @RequestBody UpdateOrderStatusDto dto) {
        return ResponseEntity.ok(orderService.updateOrderStatus(orderNumber, dto));
    }
}
