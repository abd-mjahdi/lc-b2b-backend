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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * BONUS, out of MVP scope (Phase 9). Lot certificate (origin / analysis /
 * conformity). Generation is meant to be synchronous, like invoices, without
 * a queue/worker.
 */
@Entity
@Table(
    name = "certificates",
    schema = "app",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_certificates_natural_key",
        columnNames = {"customer_number", "lot_number", "certificate_type"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Certificate {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_number", nullable = false)
    private Customer customer;

    /** Allowed values: origin, analysis, conformity */
    @ToString.Include
    @Column(name = "certificate_type", length = 20, nullable = false)
    private String certificateType;

    @ToString.Include
    @Column(name = "lot_number", length = 50, nullable = false)
    private String lotNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_code", nullable = true)
    private Product product;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    /**
     * Type-specific fields (e.g. acidity_index, peroxide_value for
     * "analysis"; country_of_origin, hs_code for "origin"). Deliberately
     * JSONB so a new certificate type never needs a schema migration.
     * Requires Hibernate 6.2+ native JSON support (org.hibernate.annotations.JdbcTypeCode).
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "technical_data", columnDefinition = "jsonb")
    private Map<String, Object> technicalData;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = true)
    private Document document;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
