package com.lesieurcristal.b2bportal.notification.controller;

import com.lesieurcristal.b2bportal.config.OpenApiConfig;
import com.lesieurcristal.b2bportal.notification.dto.NotificationDto;
import com.lesieurcristal.b2bportal.notification.service.PortalNotificationService;
import com.lesieurcristal.b2bportal.notification.service.SseNotificationBroker;
import com.lesieurcristal.b2bportal.security.AuthenticatedUser;
import com.lesieurcristal.b2bportal.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@Tag(name = "Notifications", description = "Notifications contextuelles in-app (PRD §2.2)")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final PortalNotificationService notificationService;
    private final SseNotificationBroker sseBroker;

    @Operation(summary = "Liste des notifications de l'utilisateur connecté",
            description = "Trie par date décroissante. Un client ne voit que ses propres notifications ; un admin voit les siennes + le broadcast.")
    @GetMapping
    public ResponseEntity<List<NotificationDto>> list() {
        return ResponseEntity.ok(notificationService.getNotificationsForCurrentUser());
    }

    @Operation(summary = "Compteur de notifications non lues")
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount() {
        return ResponseEntity.ok(Map.of("unread", notificationService.countUnreadForCurrentUser()));
    }

    @Operation(summary = "Marque une notification comme lue")
    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Marque toutes les notifications comme lues")
    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead() {
        notificationService.markAllAsReadForCurrentUser();
        return ResponseEntity.noContent().build();
    }

    /**
     * Endpoint SSE sécurisé (PRD §4.2). Chaque utilisateur authentifié
     * ouvre un flux qui ne reçoit que ses propres notifications.
     */
    @Operation(summary = "Flux SSE temps réel des notifications",
            description = "À chaque nouvelle notification destinée à l'utilisateur, un événement 'notification' est poussé.")
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        AuthenticatedUser current = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Non authentifié"));
        return sseBroker.register(current.getId(), current.getRole());
    }
}