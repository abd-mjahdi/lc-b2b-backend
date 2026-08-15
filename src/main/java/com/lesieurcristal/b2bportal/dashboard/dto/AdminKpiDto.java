package com.lesieurcristal.b2bportal.dashboard.dto;

/**
 * Indicateurs clés du tableau de bord admin (PRD §3.2.1).
 */
public record AdminKpiDto(
        /** Commandes ERP encore au statut logistique {@code confirmed}. */
        long pendingOrders,
        long pendingSamplesToShip,
        long activeInvoiceDisputes,
        long newClientClaims
) {
}
