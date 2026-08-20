package com.lesieurcristal.b2bportal.order.connector;

import java.util.List;

public record ErpSubmitOrderResult(
        String orderGroupId,
        List<String> orderNumbers
) {}
