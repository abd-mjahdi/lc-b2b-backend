package com.lesieurcristal.b2bportal.order.connector;

import com.lesieurcristal.b2bportal.entity.erpmock.Invoice;
import com.lesieurcristal.b2bportal.entity.erpmock.Order;
import com.lesieurcristal.b2bportal.entity.erpmock.OrderStatus;
import com.lesieurcristal.b2bportal.order.dto.OrderResponseDto;
import com.lesieurcristal.b2bportal.order.dto.OrderStatusResponseDto;
import com.lesieurcristal.b2bportal.repository.OrderRepository;
import com.lesieurcristal.b2bportal.repository.OrderStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MockErpOrderConnector implements ErpOrderConnector {

    private final OrderRepository orderRepository;
    private final OrderStatusRepository orderStatusRepository;

    @Override
    public org.springframework.data.domain.Page<OrderResponseDto> getOrdersByCustomerNumber(
            String customerNumber,
            java.time.LocalDate startDate,
            java.time.LocalDate endDate,
            String status,
            String invoiceStatus,
            org.springframework.data.domain.Pageable pageable) {

        if (customerNumber == null || customerNumber.isBlank()) {
            return org.springframework.data.domain.Page.empty(pageable);
        }
        
        org.springframework.data.domain.Page<Order> ordersPage = orderRepository.findFilteredOrders(
                customerNumber, startDate, endDate, status, invoiceStatus, pageable);

        return ordersPage.map(this::mapToOrderResponseDto);
    }

    @Override
    public Optional<OrderStatusResponseDto> getOrderStatus(String orderNumber) {
        if (orderNumber == null || orderNumber.isBlank()) {
            return Optional.empty();
        }
        return orderStatusRepository.findByOrderNumber(orderNumber)
                .map(this::mapToOrderStatusResponseDto);
    }

    private OrderResponseDto mapToOrderResponseDto(Order order) {
        Invoice invoice = order.getInvoice();
        OrderStatus status = order.getOrderStatus();
        return new OrderResponseDto(
                order.getOrderNumber(),
                order.getOrderDate(),
                order.getCustomer() != null ? order.getCustomer().getCustomerNumber() : null,
                order.getCustomerOrderReference(),
                order.getProductCode(),
                order.getProductLabel(),
                order.getQuantityOrdered(),
                order.getQuantityShipped(),
                order.getSalesUnit(),
                order.getNetAmount(),
                order.getCurrency(),
                order.getShipToCity(),
                order.getShipToCountry(),
                order.getRequestedDeliveryDate(),
                order.getPlannedDeliveryDate(),
                order.getGoodsIssueDate(),
                invoice != null ? invoice.getInvoiceNumber() : null,
                invoice != null ? invoice.getInvoiceDate() : null,
                invoice != null ? invoice.getInvoiceStatus() : null,
                invoice != null ? invoice.getTotalAmount() : null,
                invoice != null ? invoice.getDueDate() : null,
                status != null ? status.getCurrentStatus() : null
        );
    }

    private OrderStatusResponseDto mapToOrderStatusResponseDto(OrderStatus orderStatus) {
        return new OrderStatusResponseDto(
                orderStatus.getOrderNumber(),
                orderStatus.getCurrentStatus(),
                orderStatus.getStatusUpdatedAt(),
                orderStatus.getExpectedDeliveryDate(),
                orderStatus.getCarrierName(),
                orderStatus.getCarrierReference()
        );
    }
}
