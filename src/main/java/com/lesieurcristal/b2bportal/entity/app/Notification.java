package com.lesieurcristal.b2bportal.entity.app;

import com.lesieurcristal.b2bportal.entity.app.enums.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/**
 * Notification contextuelle du portail (PRD §2.2). Stockée en base et
 * poussée en temps réel sur le tableau de bord de l'utilisateur via
 * un endpoint SSE.
 *
 * <p>Une notification peut viser :
 * <ul>
 *   <li>Un utilisateur client précis ({@link #recipientUser} non null
 *       et {@link #recipientRole} = CLIENT).</li>
 *   <li>Tous les administrateurs ({@link #recipientUser} null et
 *       {@link #recipientRole} = ADMIN) — la diffusion est faite par
 *       le service.</li>
 * </ul>
 */
@Entity
@Table(name = "notifications", schema = "app")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Notification {

    @Id
    @EqualsAndHashCode.Include
    @ToString.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    /** NULL si la notification est destinée à tous les admins. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_user_id", nullable = true)
    private User recipientUser;

    @ToString.Include
    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_role", length = 20, nullable = false)
    private UserRole recipientRole;

    @ToString.Include
    @Column(name = "title", length = 150, nullable = false)
    private String title;

    @Column(name = "message", columnDefinition = "TEXT", nullable = false)
    private String message;

    /** Ex : ORDER_CREATED, SAMPLE_REQUESTED, STATUS_CHANGED, DISPUTE_OPENED, DISPUTE_RESOLVED. */
    @ToString.Include
    @Column(name = "type", length = 50, nullable = false)
    private String type;

    @Column(name = "related_entity_type", length = 50)
    private String relatedEntityType;

    @Column(name = "related_entity_id", length = 50)
    private String relatedEntityId;

    /** URL de redirection rapide (ex : /admin/invoices/disputes/42). */
    @Column(name = "target_url", length = 255)
    private String targetUrl;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
