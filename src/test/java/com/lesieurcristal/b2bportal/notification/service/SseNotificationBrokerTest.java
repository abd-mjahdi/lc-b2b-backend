package com.lesieurcristal.b2bportal.notification.service;

import com.lesieurcristal.b2bportal.entity.app.enums.UserRole;
import com.lesieurcristal.b2bportal.notification.dto.NotificationDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SseNotificationBrokerTest {

    private SseNotificationBroker broker;

    @BeforeEach
    void setUp() {
        broker = new SseNotificationBroker();
    }

    @Test
    void adminRegister_subscribesToUserKeyAndBroadcastChannel() {
        SseEmitter emitter = broker.register(7L, UserRole.ADMIN);

        assertThat(emitter).isNotNull();
        assertThat(broker.emitterCount("a:7")).isEqualTo(1);
        assertThat(broker.emitterCount(SseNotificationBroker.ADMIN_BROADCAST_KEY)).isEqualTo(1);
    }

    @Test
    void clientRegister_doesNotSubscribeToAdminBroadcast() {
        broker.register(42L, UserRole.CLIENT);

        assertThat(broker.emitterCount("c:42")).isEqualTo(1);
        assertThat(broker.emitterCount(SseNotificationBroker.ADMIN_BROADCAST_KEY)).isZero();
    }

    @Test
    void adminBroadcast_reachesEmittersOnAllChannel() {
        broker.register(1L, UserRole.ADMIN);
        broker.register(2L, UserRole.ADMIN);
        assertThat(broker.emitterCount(SseNotificationBroker.ADMIN_BROADCAST_KEY)).isEqualTo(2);

        NotificationDto dto = new NotificationDto(
                99L,
                "ADMIN",
                null,
                "Nouvelle commande",
                "Commande reçue",
                "ORDER_CREATED",
                "ORDER",
                "4500010001",
                "/admin/disputes",
                false,
                OffsetDateTime.now()
        );

        // Should not throw; both admins are on a:all
        broker.broadcast(dto);
        assertThat(broker.emitterCount(SseNotificationBroker.ADMIN_BROADCAST_KEY)).isEqualTo(2);
    }
}
