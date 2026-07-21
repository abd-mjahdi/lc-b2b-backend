package com.lesieurcristal.b2bportal.entity.app;

import com.lesieurcristal.b2bportal.entity.erpmock.Customer;
import com.lesieurcristal.b2bportal.entity.erpmock.Order;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "sample_requests", schema = "app")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SampleRequest {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_number", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    @Column(name = "product_type", length = 100)
    private String productType;

    @Column(name = "quantity", precision = 14, scale = 3)
    private BigDecimal quantity;

    @Column(name = "contact_name", length = 150)
    private String contactName;

    @Column(name = "contact_address", length = 255)
    private String contactAddress;

    /** Allowed values: new, processing, fulfilled, rejected */
    @ToString.Include
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private String status = "new";

    /** Filled later, manually or via a future real SAP integration. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resulting_order_number", nullable = true)
    private Order resultingOrder;

    @CreationTimestamp
    @Column(name = "requested_at", nullable = false, updatable = false)
    private OffsetDateTime requestedAt;
}
