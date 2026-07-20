package com.lesieurcristal.b2bportal.entity.erpmock;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Live tracking status of an order. order_number is both the primary key and
 * the FK to orders, so this is mapped as a derived-identity one-to-one via
 * {@code @MapsId}. No document/PDF is ever attached here — it's a plain
 * value re-read on every call (no cache for the MVP).
 */
@Entity
@Table(name = "order_status", schema = "erp_mock")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class OrderStatus {

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "order_number", length = 20, nullable = false)
    private String orderNumber;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "order_number")
    private Order order;

    /**
     * SAP "Statut Commande".
     * Allowed values: confirmed, in_preparation, shipped, delivered, cancelled.
     */
    @ToString.Include
    @Column(name = "current_status", length = 20, nullable = false)
    @Builder.Default
    private String currentStatus = "confirmed";

    @UpdateTimestamp
    @Column(name = "status_updated_at", nullable = false)
    private OffsetDateTime statusUpdatedAt;

    /** SAP "Date de livraison prév 1" */
    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;

    /** SAP "Nom transporteur" */
    @Column(name = "carrier_name", length = 100)
    private String carrierName;

    /** SAP "Numéro de container" / "Nº de groupage" */
    @Column(name = "carrier_reference", length = 50)
    private String carrierReference;
}
