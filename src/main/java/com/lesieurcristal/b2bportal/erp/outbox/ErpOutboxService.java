package com.lesieurcristal.b2bportal.erp.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lesieurcristal.b2bportal.entity.app.ErpOutbox;
import com.lesieurcristal.b2bportal.order.connector.ErpOrderConnector;
import com.lesieurcristal.b2bportal.order.connector.ErpSubmitOrderCommand;
import com.lesieurcristal.b2bportal.order.connector.ErpSubmitOrderResult;
import com.lesieurcristal.b2bportal.repository.ErpOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Records an outbound ERP message, then dispatches it through
 * {@link ErpOrderConnector}. Mock dispatch is synchronous so the client
 * still receives order numbers in the same request. A later SAP adapter
 * can keep this class and only swap the connector.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ErpOutboxService {

    public static final String DIRECTION_OUTBOUND = "OUTBOUND";
    public static final String DIRECTION_INBOUND = "INBOUND";
    public static final String TYPE_SUBMIT_ORDER = "SUBMIT_ORDER";
    public static final String TYPE_ORDER_STATUS = "ORDER_STATUS";

    private static final int MAX_ATTEMPTS = 5;

    private final ErpOutboxRepository erpOutboxRepository;
    private final ErpOrderConnector erpOrderConnector;
    private final ObjectMapper objectMapper;

    @Transactional
    public ErpSubmitOrderResult submitOrder(ErpSubmitOrderCommand command) {
        ErpOutbox row = erpOutboxRepository.save(ErpOutbox.builder()
                .direction(DIRECTION_OUTBOUND)
                .eventType(TYPE_SUBMIT_ORDER)
                .aggregateId(command.orderGroupId())
                .payload(writeJson(command))
                .status("PENDING")
                .attemptCount(0)
                .build());
        return dispatchSubmitOrder(row);
    }

    @Transactional
    public int retryPendingOutbound() {
        List<ErpOutbox> pending = erpOutboxRepository
                .findTop20ByDirectionAndStatusInOrderByCreatedAtAsc(
                        DIRECTION_OUTBOUND, List.of("PENDING", "FAILED", "PROCESSING"));
        int sent = 0;
        for (ErpOutbox row : pending) {
            if (row.getAttemptCount() != null && row.getAttemptCount() >= MAX_ATTEMPTS) {
                continue;
            }
            if (!TYPE_SUBMIT_ORDER.equals(row.getEventType())) {
                continue;
            }
            try {
                dispatchSubmitOrder(row);
                sent++;
            } catch (RuntimeException ex) {
                log.warn("ERP outbox retry failed id={}: {}", row.getId(), ex.getMessage());
            }
        }
        return sent;
    }

    @Transactional
    public void recordInbound(String eventType, String aggregateId, Object payload) {
        erpOutboxRepository.save(ErpOutbox.builder()
                .direction(DIRECTION_INBOUND)
                .eventType(eventType)
                .aggregateId(aggregateId)
                .payload(writeJson(payload))
                .status("SENT")
                .attemptCount(1)
                .processedAt(OffsetDateTime.now())
                .build());
    }

    private ErpSubmitOrderResult dispatchSubmitOrder(ErpOutbox row) {
        row.setStatus("PROCESSING");
        row.setAttemptCount(row.getAttemptCount() == null ? 1 : row.getAttemptCount() + 1);
        erpOutboxRepository.save(row);

        try {
            ErpSubmitOrderCommand command = objectMapper.readValue(row.getPayload(), ErpSubmitOrderCommand.class);
            ErpSubmitOrderResult result = erpOrderConnector.submitOrder(command);
            row.setStatus("SENT");
            row.setResultPayload(writeJson(result));
            row.setLastError(null);
            row.setProcessedAt(OffsetDateTime.now());
            erpOutboxRepository.save(row);
            log.info("ERP outbound SENT type={} aggregate={} orders={}",
                    row.getEventType(), row.getAggregateId(), result.orderNumbers());
            return result;
        } catch (JsonProcessingException e) {
            markFailed(row, "Payload JSON invalide: " + e.getMessage());
            throw new IllegalStateException("Payload outbox invalide", e);
        } catch (RuntimeException e) {
            markFailed(row, e.getMessage());
            throw e;
        }
    }

    private void markFailed(ErpOutbox row, String error) {
        row.setStatus("FAILED");
        row.setLastError(error);
        erpOutboxRepository.save(row);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Impossible de sérialiser le message ERP", e);
        }
    }
}
