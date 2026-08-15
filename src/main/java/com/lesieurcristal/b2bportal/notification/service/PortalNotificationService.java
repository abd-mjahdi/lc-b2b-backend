package com.lesieurcristal.b2bportal.notification.service;

import com.lesieurcristal.b2bportal.entity.app.Notification;
import com.lesieurcristal.b2bportal.entity.app.User;
import com.lesieurcristal.b2bportal.entity.app.enums.UserRole;
import com.lesieurcristal.b2bportal.notification.dto.NotificationDto;
import com.lesieurcristal.b2bportal.repository.NotificationRepository;
import com.lesieurcristal.b2bportal.repository.UserRepository;
import com.lesieurcristal.b2bportal.security.AuthenticatedUser;
import com.lesieurcristal.b2bportal.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Service de notifications in-app du portail (PRD §2.2).
 *
 * <p>Toute notification est :
 * <ol>
 *   <li>Persistée en base ({@code app.notifications}).</li>
 *   <li>Diffusée via SSE aux émetteurs enregistrés.</li>
 * </ol>
 *
 * <p>Les broadcasts admin créent <strong>une ligne par admin</strong>
 * (is_read isolé par destinataire). Les lectures sont toujours filtrées
 * par {@code recipient_user_id}.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PortalNotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SseNotificationBroker broker;

    // =========================================================================
    // Écriture
    // =========================================================================

    /**
     * Crée une notification ciblée pour un utilisateur précis.
     * Génère une notification persistée et la pousse sur le SSE broker.
     */
    public NotificationDto createNotificationForUser(
            User recipient,
            String title,
            String message,
            String type,
            String relatedEntityType,
            String relatedEntityId,
            String targetUrl) {

        if (recipient == null || recipient.getId() == null) {
            throw new IllegalArgumentException("Destinataire de notification requis");
        }

        UserRole role = recipient.getRole() != null ? recipient.getRole() : UserRole.CLIENT;

        Notification saved = notificationRepository.save(Notification.builder()
                .recipientUser(recipient)
                .recipientRole(role)
                .title(title)
                .message(message)
                .type(type)
                .relatedEntityType(relatedEntityType)
                .relatedEntityId(relatedEntityId)
                .targetUrl(targetUrl)
                .isRead(false)
                .build());

        NotificationDto dto = toDto(saved);
        broker.broadcast(dto);
        log.info("Notification créée -> user={} type={} target={}", recipient.getId(), type, targetUrl);
        return dto;
    }

    /**
     * Crée une notification pour chaque administrateur (une ligne / is_read
     * par admin). Pousse chaque copie via SSE sur le canal admin ciblé.
     */
    public NotificationDto createAdminBroadcast(
            String title,
            String message,
            String type,
            String relatedEntityType,
            String relatedEntityId,
            String targetUrl) {

        List<User> admins = userRepository.findByRole(UserRole.ADMIN);
        if (admins.isEmpty()) {
            log.warn("Aucun admin pour broadcast type={}", type);
            return null;
        }

        NotificationDto first = null;
        for (User admin : admins) {
            Notification saved = notificationRepository.save(Notification.builder()
                    .recipientUser(admin)
                    .recipientRole(UserRole.ADMIN)
                    .title(title)
                    .message(message)
                    .type(type)
                    .relatedEntityType(relatedEntityType)
                    .relatedEntityId(relatedEntityId)
                    .targetUrl(targetUrl)
                    .isRead(false)
                    .build());
            NotificationDto dto = toDto(saved);
            broker.broadcast(dto);
            if (first == null) {
                first = dto;
            }
        }

        log.info("Notification broadcast admin -> type={} title={} recipients={}",
                type, title, admins.size());
        return first;
    }

    public void markAsRead(Long notificationId) {
        AuthenticatedUser current = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Non authentifié"));
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification introuvable"));
        if (n.getRecipientUser() == null || !n.getRecipientUser().getId().equals(current.getId())) {
            throw new SecurityException("Accès refusé");
        }
        n.setIsRead(true);
        notificationRepository.save(n);
    }

    public void markAllAsReadForCurrentUser() {
        AuthenticatedUser current = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Non authentifié"));
        List<Notification> mine = notificationRepository
                .findByRecipientUserIdOrderByCreatedAtDesc(current.getId());
        for (Notification n : mine) {
            if (!Boolean.TRUE.equals(n.getIsRead())) {
                n.setIsRead(true);
            }
        }
        notificationRepository.saveAll(mine);
    }

    // =========================================================================
    // Lecture
    // =========================================================================

    @Transactional(readOnly = true)
    public List<NotificationDto> getNotificationsForCurrentUser() {
        AuthenticatedUser current = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Non authentifié"));

        return notificationRepository
                .findByRecipientUserIdOrderByCreatedAtDesc(current.getId())
                .stream()
                .sorted(Comparator.comparing(Notification::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public long countUnreadForCurrentUser() {
        AuthenticatedUser current = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Non authentifié"));
        return notificationRepository.countByRecipientUserIdAndIsReadFalse(current.getId());
    }

    public Optional<User> findUser(Long id) {
        return userRepository.findById(id);
    }

    // =========================================================================
    // Mapping
    // =========================================================================

    private NotificationDto toDto(Notification n) {
        return new NotificationDto(
                n.getId(),
                n.getRecipientRole() == null ? null : n.getRecipientRole().name(),
                n.getRecipientUser() == null ? null : n.getRecipientUser().getId(),
                n.getTitle(),
                n.getMessage(),
                n.getType(),
                n.getRelatedEntityType(),
                n.getRelatedEntityId(),
                n.getTargetUrl(),
                Boolean.TRUE.equals(n.getIsRead()),
                n.getCreatedAt()
        );
    }
}
