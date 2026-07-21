package com.lesieurcristal.b2bportal.entity.erpmock;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Invoices. The PDF itself is never stored here (see {@code Document}) —
 * this table only carries the data, always read live for paid/unpaid status.
 */
@Entity
@Table(name = "invoices", schema = "erp_mock")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Invoice {

    /** SAP "Facture" — natural key, not generated. */
    @Id
    @EqualsAndHashCode.Include
    @ToString.Include
    @Column(name = "invoice_number", length = 20, nullable = false)
    private String invoiceNumber;

    /** SAP "Date création fact." */
    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_number", nullable = true)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_number", nullable = false)
    private Customer customer;

    /** SAP "Montant net facturé" */
    @Column(name = "net_amount", precision = 14, scale = 2)
    private BigDecimal netAmount;

    /** SAP "Mt TVA (Fact)" */
    @Column(name = "vat_amount", precision = 14, scale = 2)
    private BigDecimal vatAmount;

    /** net_amount + vat_amount */
    @Column(name = "total_amount", precision = 14, scale = 2)
    private BigDecimal totalAmount;

    /** SAP "Devise" */
    @Column(name = "currency", length = 3)
    private String currency;

    /** SAP "Date d'échéance (pièce compt.)" */
    @Column(name = "due_date")
    private LocalDate dueDate;

    /**
     * Portal-specific field, absent from the SAP extract (normally comes from
     * the FI-AR module, not sales); mocked here since the client explicitly
     * requires it. Allowed values: paid, unpaid, partially_paid.
     */
    @ToString.Include
    @Column(name = "invoice_status", length = 20, nullable = false)
    @Builder.Default
    private String invoiceStatus = "unpaid";

    /** Portal-specific field, nullable. */
    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
