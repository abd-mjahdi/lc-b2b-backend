package com.lesieurcristal.b2bportal.order.controller;

import com.lesieurcristal.b2bportal.order.dto.OrderResponseDto;
import com.lesieurcristal.b2bportal.order.dto.OrderStatusResponseDto;
import com.lesieurcristal.b2bportal.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * GET /api/orders : Historique des commandes du client connecté via ErpOrderConnector.
     */
    @GetMapping
    public ResponseEntity<List<OrderResponseDto>> getOrders() {
        List<OrderResponseDto> orders = orderService.getOrdersForCurrentUser();
        return ResponseEntity.ok(orders);
    }

    /**
     * GET /api/orders/{id}/status : Statut en direct d'une commande (erp_mock.order_status).
     */
    @GetMapping("/{id}/status")
    public ResponseEntity<OrderStatusResponseDto> getOrderStatus(@PathVariable("id") String orderNumber) {
        OrderStatusResponseDto status = orderService.getOrderStatus(orderNumber);
        return ResponseEntity.ok(status);
    }
}
