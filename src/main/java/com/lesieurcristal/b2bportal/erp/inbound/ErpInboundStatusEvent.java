package com.lesieurcristal.b2bportal.erp.inbound;

import java.time.OffsetDateTime;

public record ErpInboundStatusEvent(
        String orderNumber,
        String currentStatus,
        OffsetDateTime statusUpdatedAt,
        String carrierName,
        String carrierReference
) {}
