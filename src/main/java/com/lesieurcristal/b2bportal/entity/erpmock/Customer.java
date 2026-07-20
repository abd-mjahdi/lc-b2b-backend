package com.lesieurcristal.b2bportal.entity.erpmock;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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

import java.time.OffsetDateTime;

/**
 * Simulated SAP customer master (erp_mock.customers).
 * Disposable table: once real SAP access exists this schema is dropped and
 * the ERP connector points at SAP instead — nothing in {@code app} changes.
 */
@Entity
@Table(name = "customers", schema = "erp_mock")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Customer {

    /** SAP "Donneur d'ordre" / "Payeur" — natural key, not generated. */
    @Id
    @EqualsAndHashCode.Include
    @ToString.Include
    @Column(name = "customer_number", length = 20, nullable = false)
    private String customerNumber;

    /** SAP "Nom Donneur d'ordre" */
    @ToString.Include
    @Column(name = "company_name", length = 150, nullable = false)
    private String companyName;

    @Column(name = "postal_address", length = 255)
    private String postalAddress;

    /** SAP "Ville (livré)" */
    @Column(name = "city", length = 100)
    private String city;

    /** SAP "Pays (livré)" */
    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "email", length = 150)
    private String email;

    /** SAP "ID TVA Client" */
    @Column(name = "vat_id", length = 30)
    private String vatId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
