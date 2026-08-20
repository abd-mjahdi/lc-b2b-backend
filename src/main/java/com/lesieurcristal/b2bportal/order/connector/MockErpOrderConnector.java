package com.lesieurcristal.b2bportal.order.connector;

import com.lesieurcristal.b2bportal.entity.erpmock.Customer;
import com.lesieurcristal.b2bportal.entity.erpmock.Invoice;
import com.lesieurcristal.b2bportal.entity.erpmock.Order;
import com.lesieurcristal.b2bportal.entity.erpmock.OrderStatus;
import com.lesieurcristal.b2bportal.order.dto.OrderResponseDto;
import com.lesieurcristal.b2bportal.order.dto.OrderStatusResponseDto;
import com.lesieurcristal.b2bportal.repository.CustomerRepository;
import com.lesieurcristal.b2bportal.repository.OrderRepository;
import com.lesieurcristal.b2bportal.repository.OrderStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MockErpOrderConnector implements ErpOrderConnector {

    private final OrderRepository orderRepository;
    private final OrderStatusRepository orderStatusRepository;
    private final CustomerRepository customerRepository;

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

    @Override
    public boolean existsCustomerOrderReference(String customerNumber, String customerOrderReference) {
        if (customerNumber == null || customerOrderReference == null || customerOrderReference.isBlank()) {
            return false;
        }
        return orderRepository.existsByCustomer_CustomerNumberAndCustomerOrderReference(
                customerNumber, customerOrderReference);
    }

    @Override
    @Transactional
    public ErpSubmitOrderResult submitOrder(ErpSubmitOrderCommand command) {
        List<Order> existing = orderRepository.findByOrderGroupIdWithInvoiceAndStatus(command.orderGroupId());
        if (!existing.isEmpty()) {
            return new ErpSubmitOrderResult(
                    command.orderGroupId(),
                    existing.stream().map(Order::getOrderNumber).toList());
        }

        Customer customer = customerRepository.findById(command.customerNumber())
                .orElseThrow(() -> new IllegalStateException("Client ERP inconnu : " + command.customerNumber()));

        List<String> orderNumbers = new ArrayList<>();
        OffsetDateTime now = OffsetDateTime.now();

        for (ErpSubmitOrderCommand.Line line : command.lines()) {
            Long next = orderRepository.nextOrderNumberSeq();
            if (next == null) {
                throw new IllegalStateException("Impossible d'allouer un numéro de commande");
            }
            String orderNumber = String.valueOf(next);

            Order saved = orderRepository.save(Order.builder()
                    .orderNumber(orderNumber)
                    .orderGroupId(command.orderGroupId())
                    .orderDate(command.orderDate())
                    .customer(customer)
                    .customerOrderReference(command.customerOrderReference())
                    .productCode(line.productCode())
                    .productLabel(line.productLabel())
                    .quantityOrdered(line.quantity())
                    .quantityShipped(BigDecimal.ZERO)
                    .salesUnit(line.salesUnit() != null ? line.salesUnit() : "CAR")
                    .netAmount(line.netAmount())
                    .currency(line.currency())
                    .shipToCity(command.shipToCity())
                    .shipToCountry(command.shipToCountry())
                    .transportMethod(command.transportMethod())
                    .requestedDeliveryDate(command.requestedDeliveryDate())
                    .plannedDeliveryDate(command.requestedDeliveryDate())
                    .goodsIssueDate(null)
                    .build());

            orderStatusRepository.save(OrderStatus.builder()
                    .order(saved)
                    .orderNumber(saved.getOrderNumber())
                    .currentStatus("confirmed")
                    .statusUpdatedAt(now)
                    .expectedDeliveryDate(command.requestedDeliveryDate())
                    .carrierName(null)
                    .carrierReference(null)
                    .build());

            orderNumbers.add(orderNumber);
        }

        return new ErpSubmitOrderResult(command.orderGroupId(), List.copyOf(orderNumbers));
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
