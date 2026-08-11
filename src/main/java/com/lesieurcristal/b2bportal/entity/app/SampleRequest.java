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

/**
 * Demande d'échantillon d'un client B2B. Depuis le PRD §2.1, une
 * demande d'échantillon peut être :
 * <ul>
 *   <li><b>Isolée</b> : livraison séparée, on remplit uniquement
 *       {@link #resultingOrder} quand le client transforme l'essai en
 *       commande réelle — c'est ce qui permet de mesurer le taux de
 *       conversion commerciale.</li>
 *   <li><b>Liée à une commande groupée</b> : si le client coche
 *       « joindre des échantillons à cette livraison », plusieurs
 *       enregistrements partagent la même valeur dans
 *       {@link #linkedOrderNumber} pour économiser les frais de port.</li>
 * </ul>
 */
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

    /**
     * Référence produit du catalogue {@link Product}. Une demande
     * d'échantillon pointe désormais sur un produit réel du catalogue
     * (PRD §4.1 : dropColumn product_type, ajout de product_code FK).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_code", nullable = true)
    private Product product;

    /**
     * Conservé pour rétro-compatibilité (libellé texte historique).
     * Nullable depuis la migration V5.
     */
    @Column(name = "product_type", length = 100)
    private String productType;

    @Column(name = "quantity", precision = 14, scale = 3)
    private BigDecimal quantity;

    @Column(name = "contact_name", length = 150)
    private String contactName;

    @Column(name = "contact_address", length = 255)
    private String contactAddress;

    /** Valeurs autorisées : new, processing, fulfilled, rejected. */
    @ToString.Include
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private String status = "new";

    /**
     * Numéro de commande ERP finale générée lorsque l'échantillon a
     * conduit à une vraie vente. Rempli manuellement par l'admin
     * ou automatiquement par une future intégration SAP.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resulting_order_number", nullable = true)
    private Order resultingOrder;

    /**
     * Numéro de commande auquel cet échantillon a été rattaché pour
     * expédition groupée. Quand rempli, l'échantillon voyage dans le
     * même colis que la commande — pas de frais de port additionnels.
     */
    @Column(name = "linked_order_number", length = 20, nullable = true)
    private String linkedOrderNumber;

    @CreationTimestamp
    @Column(name = "requested_at", nullable = false, updatable = false)
    private OffsetDateTime requestedAt;
}
