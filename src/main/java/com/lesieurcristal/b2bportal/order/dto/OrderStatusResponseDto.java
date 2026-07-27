package com.lesieurcristal.b2bportal.order.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record OrderStatusResponseDto(
        String orderNumber,
        String currentStatus,
        OffsetDateTime statusUpdatedAt,
        LocalDate expectedDeliveryDate,
        String carrierName,
        String carrierReference
) {
}
