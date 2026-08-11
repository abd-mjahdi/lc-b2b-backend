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
 * <p>L'isolation par client est appliquée lors des lectures :
 * un client ne récupère que ses propres notifications (par userId)
 * tandis qu'un admin récupère les siennes + le broadcast.</p>
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

        Notification saved = notificationRepository.save(Notification.builder()
                .recipientUser(recipient)
                .recipientRole(UserRole.CLIENT)
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
     * Crée une notification broadcast pour tous les administrateurs.
     */
    public NotificationDto createAdminBroadcast(
            String title,
            String message,
            String type,
            String relatedEntityType,
            String relatedEntityId,
            String targetUrl) {

        Notification saved = notificationRepository.save(Notification.builder()
                .recipientUser(null)
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
        log.info("Notification broadcast admin -> type={} title={}", type, title);
        return dto;
    }

    public void markAsRead(Long notificationId) {
        AuthenticatedUser current = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Non authentifié"));
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification introuvable"));
        // Isolation : un client ne peut marquer que ses propres notifications
        if (current.getRole() == UserRole.CLIENT) {
            if (n.getRecipientUser() == null || !n.getRecipientUser().getId().equals(current.getId())) {
                throw new SecurityException("Accès refusé");
            }
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

        List<Notification> mine = notificationRepository
                .findByRecipientUserIdOrderByCreatedAtDesc(current.getId());

        if (current.getRole() == UserRole.ADMIN) {
            List<Notification> broadcast = notificationRepository
                    .findByRecipientRoleAndRecipientUserIsNullOrderByCreatedAtDesc("ADMIN");
            // concat en gardant l'ordre décroissant : la liste mine est déjà triée,
            // on intercale mais le tri final est assuré par la base.
            mine.addAll(broadcast);
        }
        return mine.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public long countUnreadForCurrentUser() {
        AuthenticatedUser current = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Non authentifié"));
        long mine = notificationRepository.countByRecipientUserIdAndIsReadFalse(current.getId());
        if (current.getRole() == UserRole.ADMIN) {
            long broadcast = notificationRepository
                    .countByRecipientRoleAndRecipientUserIsNullAndIsReadFalse("ADMIN");
            return mine + broadcast;
        }
        return mine;
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