package com.lesieurcristal.b2bportal.order.connector;

import com.lesieurcristal.b2bportal.order.dto.OrderResponseDto;
import com.lesieurcristal.b2bportal.order.dto.OrderStatusResponseDto;

import java.util.Optional;

/**
 * Door to ERP order data. {@link MockErpOrderConnector} talks to {@code erp_mock}.
 * A future SAP implementation keeps the same methods.
 */
public interface ErpOrderConnector {

    org.springframework.data.domain.Page<OrderResponseDto> getOrdersByCustomerNumber(
            String customerNumber,
            java.time.LocalDate startDate,
            java.time.LocalDate endDate,
            String status,
            String invoiceStatus,
            org.springframework.data.domain.Pageable pageable);

    Optional<OrderStatusResponseDto> getOrderStatus(String orderNumber);

    boolean existsCustomerOrderReference(String customerNumber, String customerOrderReference);

    /**
     * Creates the sales documents in ERP and returns allocated order numbers.
     * Must be idempotent on {@code command.orderGroupId()}.
     */
    ErpSubmitOrderResult submitOrder(ErpSubmitOrderCommand command);
}
