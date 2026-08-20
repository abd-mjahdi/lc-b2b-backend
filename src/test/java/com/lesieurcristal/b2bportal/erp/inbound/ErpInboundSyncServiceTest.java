package com.lesieurcristal.b2bportal.erp.inbound;

import com.lesieurcristal.b2bportal.entity.app.ErpSyncState;
import com.lesieurcristal.b2bportal.entity.erpmock.OrderStatus;
import com.lesieurcristal.b2bportal.erp.outbox.ErpOutboxService;
import com.lesieurcristal.b2bportal.repository.ErpSyncStateRepository;
import com.lesieurcristal.b2bportal.repository.OrderStatusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErpInboundSyncServiceTest {

    @Mock
    private ErpSyncStateRepository erpSyncStateRepository;

    @Mock
    private OrderStatusRepository orderStatusRepository;

    @Mock
    private ErpOutboxService erpOutboxService;

    private ErpInboundSyncService service;

    @BeforeEach
    void setUp() {
        service = new ErpInboundSyncService(erpSyncStateRepository, orderStatusRepository, erpOutboxService);
    }

    @Test
    void firstPull_initialisesCursorWithoutImportingHistory() {
        when(erpSyncStateRepository.findById("inbound")).thenReturn(Optional.empty());
        when(erpSyncStateRepository.save(any(ErpSyncState.class))).thenAnswer(inv -> inv.getArgument(0));

        int pulled = service.pullUpdates();

        assertThat(pulled).isZero();
        verify(orderStatusRepository, never()).findByStatusUpdatedAtGreaterThanOrderByStatusUpdatedAtAsc(any());
        ArgumentCaptor<ErpSyncState> captor = ArgumentCaptor.forClass(ErpSyncState.class);
        verify(erpSyncStateRepository).save(captor.capture());
        assertThat(captor.getValue().getLastCursor()).isNotNull();
    }

    @Test
    void laterPull_recordsInboundStatusEvents() {
        OffsetDateTime cursor = OffsetDateTime.parse("2026-08-20T10:00:00+01:00");
        when(erpSyncStateRepository.findById("inbound")).thenReturn(Optional.of(
                ErpSyncState.builder().id("inbound").lastCursor(cursor).recordsPulled(0).build()));
        when(erpSyncStateRepository.save(any(ErpSyncState.class))).thenAnswer(inv -> inv.getArgument(0));

        OffsetDateTime updated = cursor.plusMinutes(5);
        OrderStatus status = OrderStatus.builder()
                .orderNumber("4500010001")
                .currentStatus("shipped")
                .statusUpdatedAt(updated)
                .build();
        when(orderStatusRepository.findByStatusUpdatedAtGreaterThanOrderByStatusUpdatedAtAsc(cursor))
                .thenReturn(List.of(status));

        int pulled = service.pullUpdates();

        assertThat(pulled).isEqualTo(1);
        verify(erpOutboxService).recordInbound(
                eq(ErpOutboxService.TYPE_ORDER_STATUS),
                eq("4500010001"),
                any(ErpInboundStatusEvent.class));
    }
}
