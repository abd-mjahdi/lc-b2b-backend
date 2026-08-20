package com.lesieurcristal.b2bportal.erp.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.lesieurcristal.b2bportal.entity.app.ErpOutbox;
import com.lesieurcristal.b2bportal.order.connector.ErpOrderConnector;
import com.lesieurcristal.b2bportal.order.connector.ErpSubmitOrderCommand;
import com.lesieurcristal.b2bportal.order.connector.ErpSubmitOrderResult;
import com.lesieurcristal.b2bportal.repository.ErpOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErpOutboxServiceTest {

    @Mock
    private ErpOutboxRepository erpOutboxRepository;

    @Mock
    private ErpOrderConnector erpOrderConnector;

    private ErpOutboxService erpOutboxService;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        erpOutboxService = new ErpOutboxService(erpOutboxRepository, erpOrderConnector, mapper);
        when(erpOutboxRepository.save(any(ErpOutbox.class))).thenAnswer(inv -> {
            ErpOutbox row = inv.getArgument(0);
            if (row.getId() == null) {
                row.setId(1L);
            }
            return row;
        });
    }

    @Test
    void submitOrder_marksOutboxSent() {
        ErpSubmitOrderCommand command = sampleCommand();
        when(erpOrderConnector.submitOrder(any(ErpSubmitOrderCommand.class)))
                .thenReturn(new ErpSubmitOrderResult("OG-1", List.of("4500010100")));

        ErpSubmitOrderResult result = erpOutboxService.submitOrder(command);

        assertThat(result.orderNumbers()).containsExactly("4500010100");
        verify(erpOutboxRepository, org.mockito.Mockito.atLeastOnce()).save(any(ErpOutbox.class));
    }

    @Test
    void submitOrder_marksOutboxFailedWhenConnectorThrows() {
        when(erpOrderConnector.submitOrder(any(ErpSubmitOrderCommand.class)))
                .thenThrow(new IllegalStateException("SAP down"));

        assertThatThrownBy(() -> erpOutboxService.submitOrder(sampleCommand()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SAP down");
    }

    private static ErpSubmitOrderCommand sampleCommand() {
        return new ErpSubmitOrderCommand(
                "OG-1",
                "CUST0001",
                "BC-1",
                LocalDate.of(2026, 8, 20),
                LocalDate.of(2026, 8, 27),
                "Casablanca",
                "Maroc",
                "camion",
                List.of(new ErpSubmitOrderCommand.Line(
                        "HSO-001",
                        "Huile de soja",
                        new BigDecimal("2"),
                        "CAR",
                        new BigDecimal("580.00"),
                        "MAD"
                ))
        );
    }
}
