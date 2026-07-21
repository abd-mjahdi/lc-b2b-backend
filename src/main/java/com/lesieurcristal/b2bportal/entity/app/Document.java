package com.lesieurcristal.b2bportal.entity.app;

import com.lesieurcristal.b2bportal.entity.erpmock.Customer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

/**
 * Single generic table backing "My documents" for every document type,
 * regardless of origin. For the MVP only invoices are written here.
 * The unique constraint enforces "generate once, then reuse" at the DB level.
 */
@Entity
@Table(
    name = "documents",
    schema = "app",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_documents_type_natural_key",
        columnNames = {"doc_type", "natural_key"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Document {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_number", nullable = false)
    private Customer customer;

    /**
     * Allowed values: invoice, certificate. Only 'invoice' is populated for
     * the MVP; 'certificate' is reserved for the Phase 9 bonus scope.
     */
    @ToString.Include
    @Column(name = "doc_type", length = 20, nullable = false)
    private String docType;

    @Column(name = "title", length = 200)
    private String title;

    /**
     * De-duplication key: an invoice number, or
     * "customerNumber:lotNumber:certificateType" for a certificate.
     */
    @ToString.Include
    @Column(name = "natural_key", length = 150, nullable = false)
    private String naturalKey;

    /** Allowed values: ready, processing, pending_review */
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private String status = "ready";

    /** Null until the document is ready. */
    @Column(name = "file_path", length = 500)
    private String filePath;

    /** Order number, lot number, etc. — traceability. */
    @Column(name = "related_reference", length = 100)
    private String relatedReference;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
