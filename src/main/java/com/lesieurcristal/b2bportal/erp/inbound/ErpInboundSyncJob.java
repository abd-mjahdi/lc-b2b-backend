package com.lesieurcristal.b2bportal.erp.inbound;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ErpInboundSyncJob {

    private final ErpInboundSyncService erpInboundSyncService;

    @Scheduled(fixedDelayString = "${app.erp.inbound-sync-ms:60000}")
    public void syncInbound() {
        try {
            erpInboundSyncService.pullUpdates();
        } catch (RuntimeException ex) {
            log.warn("ERP inbound sync failed: {}", ex.getMessage());
        }
    }
}
