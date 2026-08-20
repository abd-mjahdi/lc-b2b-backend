package com.lesieurcristal.b2bportal.order.connector;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Portal → ERP create-order command. A future SapOrderConnector maps this
 * to whatever SAP actually exposes (BAPI, IDoc, REST…).
 */
public record ErpSubmitOrderCommand(
        String orderGroupId,
        String customerNumber,
        String customerOrderReference,
        LocalDate orderDate,
        LocalDate requestedDeliveryDate,
        String shipToCity,
        String shipToCountry,
        String transportMethod,
        List<Line> lines
) {
    public record Line(
            String productCode,
            String productLabel,
            BigDecimal quantity,
            String salesUnit,
            BigDecimal netAmount,
            String currency
    ) {}
}
