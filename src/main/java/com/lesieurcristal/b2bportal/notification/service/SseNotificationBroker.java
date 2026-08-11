package com.lesieurcristal.b2bportal.notification.service;

import com.lesieurcristal.b2bportal.entity.app.enums.UserRole;
import com.lesieurcristal.b2bportal.notification.dto.NotificationDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Gestionnaire de flux Server-Sent Events (SSE) pour les notifications.
 *
 * <p>Un {@link SseEmitter} est associé à chaque couple
 * (userId, role) actuellement connecté. Lorsqu'une notification est
 * créée, on émet l'événement sur tous les émetteurs qui correspondent
 * à son destinataire.</p>
 *
 * <p>Cette classe est volontairement sans état JPA : elle ne stocke
 * rien en base. Le {@link NotificationService} persiste d'abord, puis
 * appelle {@link #broadcast(NotificationDto)}.</p>
 */
@Slf4j
@Service
public class SseNotificationBroker {

    /** Clé composite : "u:42" pour un user ciblé, "a:all" pour broadcast admins, etc. */
    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    /** Connexion d'un utilisateur identifié (client ciblé ou admin). */
    public SseEmitter register(Long userId, UserRole role) {
        SseEmitter emitter = new SseEmitter(0L); // pas de timeout serveur
        String key = keyFor(userId, role.name());
        emitters.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(key, emitter));
        emitter.onTimeout(() -> remove(key, emitter));
        emitter.onError(t -> remove(key, emitter));
        log.debug("SSE registered : {} (key={})", userId, key);
        return emitter;
    }

    /** Envoie l'événement aux émetteurs qui correspondent au destinataire. */
    public void broadcast(NotificationDto notification) {
        // 1. destinataire ciblé (utilisateur précis)
        if (notification.recipientUserId() != null) {
            send(keyFor(notification.recipientUserId(), notification.recipientRole()),
                    notification, notification.recipientUserId());
        }
        // 2. broadcast à tous les admins (recipientUserId == null && role == ADMIN)
        if (notification.recipientUserId() == null && "ADMIN".equals(notification.recipientRole())) {
            send("a:all", notification, null);
        }
    }

    private void send(String key, NotificationDto payload, Long targetedUserId) {
        List<SseEmitter> list = emitters.get(key);
        if (list == null || list.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(payload));
            } catch (IOException ex) {
                log.warn("SSE send failed, removing emitter : {}", ex.getMessage());
                remove(key, emitter);
            }
        }
    }

    private void remove(String key, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(key);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                emitters.remove(key);
            }
        }
    }

    private static String keyFor(Long userId, String role) {
        return role.toLowerCase().charAt(0) + ":" + (userId == null ? "all" : userId);
    }
}