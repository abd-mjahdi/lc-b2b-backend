package com.lesieurcristal.b2bportal.auth.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Login identifier is the account e-mail. {@code login} is accepted as a JSON
 * alias so older clients still bind the same field.
 */
public record LoginRequest(
        @NotBlank(message = "L'e-mail est obligatoire")
        @Size(max = 150)
        @JsonAlias("login")
        String email,
        @NotBlank(message = "Le mot de passe est obligatoire")
        @Size(min = 8, max = 128, message = "Le mot de passe doit contenir au moins 8 caractères")
        String password
) {
    public String identifier() {
        return email == null ? "" : email.trim();
    }
}
