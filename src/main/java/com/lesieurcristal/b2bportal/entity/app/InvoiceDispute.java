package com.lesieurcristal.b2bportal.entity.app;

import com.lesieurcristal.b2bportal.entity.erpmock.Customer;
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
 * Contestation de facture (PRD §2.3, §4.1). Une contestation ouverte
 * passe la facture ERP associée à {@code invoice_status = 'disputed'}
 * tant que l'arbitrage n'a pas été rendu.
 */
@Entity
@Table(name = "invoice_disputes", schema = "app")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class InvoiceDispute {

    @Id
    @EqualsAndHashCode.Include
    @ToString.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "invoice_number", length = 20, nullable = false)
    private String invoiceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_number", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    @ToString.Include
    @Enumerated(EnumType.STRING)
    @Column(name = "reason", length = 50, nullable = false)
    private DisputeReason reason;

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    /** Chemin du justificatif uploadé par le client (photo, BL, etc.). */
    @Column(name = "file_path", length = 500)
    private String filePath;

    @ToString.Include
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private DisputeStatus status = DisputeStatus.PENDING;

    /**
     * Snapshot of {@code erp_mock.invoices.invoice_status} before the dispute
     * flipped it to {@code disputed}. Restored on approve/reject.
     */
    @Column(name = "previous_invoice_status", length = 20)
    private String previousInvoiceStatus;

    /** Commentaire écrit par l'administrateur lors du rejet ou de l'approbation. */
    @Column(name = "resolution_note", columnDefinition = "TEXT")
    private String resolutionNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by_user_id", nullable = true)
    private User resolvedBy;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public enum DisputeReason {
        QUANTITY_DISCREPANCY,
        PRICE_DISCREPANCY,
        DAMAGED_GOODS,
        OTHER
    }

    public enum DisputeStatus {
        PENDING,
        APPROVED,
        REJECTED
    }
}
