package com.lesieurcristal.b2bportal.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Champs modifiables de la fiche client (hors données SAP verrouillées).
 * Seuls {@code phone} et {@code email} sont acceptés — les autres champs
 * (raison sociale, TVA, adresse, etc.) restent en lecture seule.
 */
public record UpdateCustomerProfileRequest(
        @Size(max = 30, message = "Le téléphone ne peut pas dépasser 30 caractères")
        String phone,

        @Email(message = "Format d'email invalide")
        @Size(max = 150, message = "L'email ne peut pas dépasser 150 caractères")
        String email
) {
}
