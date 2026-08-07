package com.lesieurcristal.b2bportal.entity.app;

import com.lesieurcristal.b2bportal.entity.app.enums.UserRole;
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
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

/**
 * Login accounts (CLIENT and ADMIN). No public self-registration for this
 * MVP — accounts are provisioned manually / via seed data.
 *
 * <p>Note: the DB enforces {@code CHECK (role = 'ADMIN' OR customer_number
 * IS NOT NULL)} at the table level. JPA/Hibernate cannot express this
 * cross-field constraint declaratively — validate it in the service layer
 * (e.g. a Bean Validation class-level constraint) before persisting.</p>
 */
@Entity
@Table(name = "users", schema = "app")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class User {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    /** Null for an ADMIN account (not attached to a customer). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_number", nullable = true)
    private Customer customer;

    @Column(name = "last_name", length = 100, nullable = false)
    private String lastName;

    @Column(name = "first_name", length = 100, nullable = false)
    private String firstName;

    @ToString.Include
    @Column(name = "email", length = 150, nullable = false, unique = true)
    private String email;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "login", length = 50, nullable = false, unique = true)
    private String login;

    @Column(name = "password_hash", length = 255, nullable = false)
    private String passwordHash;

    @ToString.Include
    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 20, nullable = false)
    private UserRole role;

    @Column(name = "language", length = 5, nullable = false)
    @Builder.Default
    private String language = "fr";

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_deactivated", nullable = false)
    @Builder.Default
    private Boolean isDeactivated = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
