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
 * Order lines simulating a SAP extract ("Historique des commandes").
 * Only the columns with client-facing display value were kept from the
 * original ~180-column SAP report.
 */
@Entity
@Table(name = "orders", schema = "erp_mock")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Order {

    /** SAP "Document de vente" — natural key, not generated. */
    @Id
    @EqualsAndHashCode.Include
    @ToString.Include
    @Column(name = "order_number", length = 20, nullable = false)
    private String orderNumber;

    /** SAP "Date doc." */
    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_number", nullable = false)
    private Customer customer;

    /** SAP "Cde Client" — the client's own internal order number. */
    @Column(name = "customer_order_reference", length = 50)
    private String customerOrderReference;

    /** SAP "Article" */
    @Column(name = "product_code", length = 30, nullable = false)
    private String productCode;

    /** SAP "Libellé" */
    @Column(name = "product_label", length = 150)
    private String productLabel;

    /** SAP "Qté cdée de cde" */
    @Column(name = "quantity_ordered", precision = 14, scale = 3)
    private BigDecimal quantityOrdered;

    /** SAP "Quantité expédiée" */
    @Column(name = "quantity_shipped", precision = 14, scale = 3)
    private BigDecimal quantityShipped;

    /** SAP "Unité de vente" */
    @Column(name = "sales_unit", length = 10)
    private String salesUnit;

    /** SAP "Montant net" / "Val. Nette" */
    @Column(name = "net_amount", precision = 14, scale = 2)
    private BigDecimal netAmount;

    /** SAP "Devise" */
    @Column(name = "currency", length = 3)
    private String currency;

    /** SAP "Ville (livré)" */
    @Column(name = "ship_to_city", length = 100)
    private String shipToCity;

    /** SAP "Pays (livré)" */
    @Column(name = "ship_to_country", length = 100)
    private String shipToCountry;

    /** SAP "Date liv." */
    @Column(name = "requested_delivery_date")
    private LocalDate requestedDeliveryDate;

    /** SAP "Date de livraison prév 1" */
    @Column(name = "planned_delivery_date")
    private LocalDate plannedDeliveryDate;

    /** SAP "Date SM réelle" — actual shipment date. */
    @Column(name = "goods_issue_date")
    private LocalDate goodsIssueDate;

    /**
     * SAP "Facture". FK added via ALTER TABLE and DEFERRABLE INITIALLY DEFERRED
     * in the schema, since an order can be created before its invoice exists.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_number", nullable = true)
    private Invoice invoice;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
