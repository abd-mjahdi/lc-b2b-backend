package com.lesieurcristal.b2bportal.order.connector;

import com.lesieurcristal.b2bportal.order.dto.OrderResponseDto;
import com.lesieurcristal.b2bportal.order.dto.OrderStatusResponseDto;

import java.util.List;
import java.util.Optional;

public interface ErpOrderConnector {

    /**
     * Retrieve order history for a given customer number, including invoice status, with pagination and filtering.
     *
     * @param customerNumber customer identification number in ERP
     * @param startDate filter orders from this date
     * @param endDate filter orders to this date
     * @param status filter by invoice status
     * @param pageable pagination and sorting information
     * @return paginated list of orders for the specified customer
     */
    org.springframework.data.domain.Page<OrderResponseDto> getOrdersByCustomerNumber(
            String customerNumber,
            java.time.LocalDate startDate,
            java.time.LocalDate endDate,
            String status,
            org.springframework.data.domain.Pageable pageable);

    /**
     * Retrieve live order status directly from ERP (no caching).
     *
     * @param orderNumber order identification number
     * @return optional order status details if found
     */
    Optional<OrderStatusResponseDto> getOrderStatus(String orderNumber);
}
