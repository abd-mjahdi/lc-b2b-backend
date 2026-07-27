package com.lesieurcristal.b2bportal.order.connector;

import com.lesieurcristal.b2bportal.order.dto.OrderResponseDto;
import com.lesieurcristal.b2bportal.order.dto.OrderStatusResponseDto;

import java.util.List;
import java.util.Optional;

public interface ErpOrderConnector {

    /**
     * Retrieve order history for a given customer number, including invoice status.
     *
     * @param customerNumber customer identification number in ERP
     * @return list of orders for the specified customer
     */
    List<OrderResponseDto> getOrdersByCustomerNumber(String customerNumber);

    /**
     * Retrieve live order status directly from ERP (no caching).
     *
     * @param orderNumber order identification number
     * @return optional order status details if found
     */
    Optional<OrderStatusResponseDto> getOrderStatus(String orderNumber);
}
