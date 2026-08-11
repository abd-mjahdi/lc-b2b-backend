package com.lesieurcristal.b2bportal.entity.app;

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

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Catalogue statique utilisé par les devis, les échantillons et les
 * commandes. Depuis le PRD §2.1, chaque produit porte un drapeau
 * {@code isSampleable} pour indiquer qu'il peut être demandé comme
 * échantillon, ainsi qu'une quantité maximale autorisée en échantillon.
 */
@Entity
@Table(name = "products", schema = "app")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Product {

    @Id
    @EqualsAndHashCode.Include
    @ToString.Include
    @Column(name = "code", length = 30, nullable = false)
    private String code;

    @ToString.Include
    @Column(name = "name", length = 150, nullable = false)
    private String name;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ToString.Include
    @Column(name = "is_sampleable", nullable = false)
    @Builder.Default
    private Boolean isSampleable = false;

    /** Quantité maximale qu'un client peut demander en échantillon. */
    @Column(name = "max_sample_quantity", precision = 14, scale = 3)
    private BigDecimal maxSampleQuantity;

    /** Prix unitaire de vente (référence catalogue). */
    @Column(name = "unit_price", precision = 14, scale = 2)
    private BigDecimal unitPrice;

    /** Unité de vente : CAR (carton), BTL (bouteille), etc. */
    @Column(name = "sales_unit", length = 10)
    private String salesUnit;

    /** URL d'image produit pour le catalogue. */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
