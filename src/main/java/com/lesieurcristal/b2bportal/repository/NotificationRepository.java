package com.lesieurcristal.b2bportal.repository;

import com.lesieurcristal.b2bportal.entity.app.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** Toutes les notifications d'un utilisateur précis (toutes rôles confondus). */
    List<Notification> findByRecipientUserIdOrderByCreatedAtDesc(Long recipientUserId);

    /** Notifications destinées à un utilisateur ET filtrées par son rôle. */
    List<Notification> findByRecipientUserIdAndRecipientRoleOrderByCreatedAtDesc(Long userId, String recipientRole);

    /** Notifications broadcast réservées à tous les admins. */
    List<Notification> findByRecipientRoleAndRecipientUserIsNullOrderByCreatedAtDesc(String recipientRole);

    /** Compte des notifications non lues pour un utilisateur. */
    long countByRecipientUserIdAndIsReadFalse(Long recipientUserId);

    /** Compte des notifications broadcast non lues. */
    long countByRecipientRoleAndRecipientUserIsNullAndIsReadFalse(String recipientRole);
}
