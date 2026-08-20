package com.lesieurcristal.b2bportal.erp.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ErpOutboundRetryJob {

    private final ErpOutboxService erpOutboxService;

    @Scheduled(fixedDelayString = "${app.erp.outbound-retry-ms:30000}")
    public void retryOutbound() {
        int sent = erpOutboxService.retryPendingOutbound();
        if (sent > 0) {
            log.info("ERP outbox retry: {} message(s) renvoyé(s)", sent);
        }
    }
}
