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
 * <p>Clés d'abonnement :
 * <ul>
 *   <li>{@code c:{userId}} / {@code a:{userId}} — notifications ciblées</li>
 * <li>{@code a:all} — broadcast admin (orders, samples, disputes, claims…)</li>
 * </ul>
 * Les admins s'abonnent aux deux canaux. Les broadcasts admin sont aussi
 * poussés sur {@code a:{userId}} (une ligne notification par admin).</p>
 */
@Slf4j
@Service
public class SseNotificationBroker {

    static final String ADMIN_BROADCAST_KEY = "a:all";

    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    /** Connexion d'un utilisateur identifié (client ciblé ou admin). */
    public SseEmitter register(Long userId, UserRole role) {
        SseEmitter emitter = new SseEmitter(0L); // pas de timeout serveur
        String userKey = keyFor(userId, role.name());
        addEmitter(userKey, emitter);

        // Les admins doivent aussi écouter le canal broadcast a:all
        // (legacy shared broadcasts / future a:all pushes).
        if (role == UserRole.ADMIN) {
            addEmitter(ADMIN_BROADCAST_KEY, emitter);
            Runnable cleanup = () -> {
                remove(userKey, emitter);
                remove(ADMIN_BROADCAST_KEY, emitter);
            };
            emitter.onCompletion(cleanup);
            emitter.onTimeout(cleanup);
            emitter.onError(t -> cleanup.run());
        } else {
            emitter.onCompletion(() -> remove(userKey, emitter));
            emitter.onTimeout(() -> remove(userKey, emitter));
            emitter.onError(t -> remove(userKey, emitter));
        }

        log.debug("SSE registered : {} (key={}, adminBroadcast={})",
                userId, userKey, role == UserRole.ADMIN);
        return emitter;
    }

    /** Envoie l'événement aux émetteurs qui correspondent au destinataire. */
    public void broadcast(NotificationDto notification) {
        // 1. destinataire ciblé (utilisateur précis)
        if (notification.recipientUserId() != null) {
            send(keyFor(notification.recipientUserId(), notification.recipientRole()), notification);
        }
        // 2. broadcast à tous les admins (recipientUserId == null && role == ADMIN)
        if (notification.recipientUserId() == null && "ADMIN".equals(notification.recipientRole())) {
            send(ADMIN_BROADCAST_KEY, notification);
        }
    }

    /** Visible for tests — number of live emitters on a key. */
    int emitterCount(String key) {
        List<SseEmitter> list = emitters.get(key);
        return list == null ? 0 : list.size();
    }

    private void addEmitter(String key, SseEmitter emitter) {
        emitters.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(emitter);
    }

    private void send(String key, NotificationDto payload) {
        List<SseEmitter> list = emitters.get(key);
        if (list == null || list.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : List.copyOf(list)) {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(payload));
            } catch (IOException ex) {
                log.warn("SSE send failed, removing emitter : {}", ex.getMessage());
                remove(key, emitter);
                // Also drop from a:all if this was a dual-registered admin emitter
                if (!ADMIN_BROADCAST_KEY.equals(key)) {
                    remove(ADMIN_BROADCAST_KEY, emitter);
                }
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

    static String keyFor(Long userId, String role) {
        if (role == null || role.isBlank()) {
            return "u:" + (userId == null ? "all" : userId);
        }
        return Character.toLowerCase(role.charAt(0)) + ":" + (userId == null ? "all" : userId);
    }
}
