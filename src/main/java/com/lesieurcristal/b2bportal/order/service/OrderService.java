package com.lesieurcristal.b2bportal.order.service;

import com.lesieurcristal.b2bportal.entity.app.enums.UserRole;
import com.lesieurcristal.b2bportal.entity.erpmock.Order;
import com.lesieurcristal.b2bportal.order.OrderException;
import com.lesieurcristal.b2bportal.order.connector.ErpOrderConnector;
import com.lesieurcristal.b2bportal.order.dto.OrderResponseDto;
import com.lesieurcristal.b2bportal.order.dto.OrderStatusResponseDto;
import com.lesieurcristal.b2bportal.repository.OrderRepository;
import com.lesieurcristal.b2bportal.security.AuthenticatedUser;
import com.lesieurcristal.b2bportal.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final ErpOrderConnector erpOrderConnector;
    private final OrderRepository orderRepository;

    /**
     * Fetch order history for the currently logged-in customer.
     */
    public org.springframework.data.domain.Page<OrderResponseDto> getOrdersForCurrentUser(
            java.time.LocalDate startDate,
            java.time.LocalDate endDate,
            String status,
            org.springframework.data.domain.Pageable pageable) {
        
        AuthenticatedUser currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(OrderException::accessDenied);

        String customerNumber = currentUser.getCustomerNumber();
        if (customerNumber == null || customerNumber.isBlank()) {
            throw OrderException.noCustomerAssociated();
        }

        return erpOrderConnector.getOrdersByCustomerNumber(customerNumber, startDate, endDate, status, pageable);
    }

    /**
     * Fetch order history for a specific customer (Admin use).
     */
    public org.springframework.data.domain.Page<OrderResponseDto> getOrdersForCustomer(
            String customerNumber,
            java.time.LocalDate startDate,
            java.time.LocalDate endDate,
            String status,
            org.springframework.data.domain.Pageable pageable) {
        
        return erpOrderConnector.getOrdersByCustomerNumber(customerNumber, startDate, endDate, status, pageable);
    }

    /**
     * Fetch live status for a specific order. Enforces data isolation between clients.
     */
    public OrderStatusResponseDto getOrderStatus(String orderNumber) {
        AuthenticatedUser currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(OrderException::accessDenied);

        Order order = orderRepository.findByOrderNumberWithInvoice(orderNumber)
                .orElseThrow(() -> OrderException.notFound(orderNumber));

        // Data isolation check: client can only view status of their own orders
        if (currentUser.getRole() == UserRole.CLIENT) {
            String currentUserCustomerNumber = currentUser.getCustomerNumber();
            if (order.getCustomer() == null || !order.getCustomer().getCustomerNumber().equals(currentUserCustomerNumber)) {
                throw OrderException.notFound(orderNumber);
            }
        }

        return erpOrderConnector.getOrderStatus(orderNumber)
                .orElseThrow(() -> OrderException.notFound(orderNumber));
    }
}
