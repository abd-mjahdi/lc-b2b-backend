package com.lesieurcristal.b2bportal.erp.inbound;

import com.lesieurcristal.b2bportal.entity.app.ErpSyncState;
import com.lesieurcristal.b2bportal.entity.erpmock.OrderStatus;
import com.lesieurcristal.b2bportal.erp.outbox.ErpOutboxService;
import com.lesieurcristal.b2bportal.repository.ErpSyncStateRepository;
import com.lesieurcristal.b2bportal.repository.OrderStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Pulls ERP status changes into the portal. Mock source = {@code erp_mock.order_status}.
 * A real SAP job would call the SAP API and upsert local copies instead.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ErpInboundSyncService {

    public static final String SYNC_ID = "inbound";

    private final ErpSyncStateRepository erpSyncStateRepository;
    private final OrderStatusRepository orderStatusRepository;
    private final ErpOutboxService erpOutboxService;

    @Transactional
    public int pullUpdates() {
        OffsetDateTime now = OffsetDateTime.now();
        ErpSyncState state = erpSyncStateRepository.findById(SYNC_ID)
                .orElseGet(() -> ErpSyncState.builder().id(SYNC_ID).recordsPulled(0).build());
        state.setLastRunAt(now);

        try {
            if (state.getLastCursor() == null) {
                state.setLastCursor(now);
                state.setLastSuccessAt(now);
                state.setLastError(null);
                state.setRecordsPulled(0);
                erpSyncStateRepository.save(state);
                log.info("ERP inbound cursor initialised — historical mock statuses skipped");
                return 0;
            }

            List<OrderStatus> changes = orderStatusRepository
                    .findByStatusUpdatedAtGreaterThanOrderByStatusUpdatedAtAsc(state.getLastCursor());

            OffsetDateTime maxCursor = state.getLastCursor();
            for (OrderStatus status : changes) {
                erpOutboxService.recordInbound(
                        ErpOutboxService.TYPE_ORDER_STATUS,
                        status.getOrderNumber(),
                        new ErpInboundStatusEvent(
                                status.getOrderNumber(),
                                status.getCurrentStatus(),
                                status.getStatusUpdatedAt(),
                                status.getCarrierName(),
                                status.getCarrierReference()
                        ));
                if (status.getStatusUpdatedAt() != null && status.getStatusUpdatedAt().isAfter(maxCursor)) {
                    maxCursor = status.getStatusUpdatedAt();
                }
            }

            state.setLastCursor(maxCursor);
            state.setLastSuccessAt(now);
            state.setLastError(null);
            state.setRecordsPulled(changes.size());
            erpSyncStateRepository.save(state);

            if (!changes.isEmpty()) {
                log.info("ERP inbound pulled {} status update(s)", changes.size());
            }
            return changes.size();
        } catch (RuntimeException ex) {
            state.setLastError(ex.getMessage());
            erpSyncStateRepository.save(state);
            throw ex;
        }
    }
}
