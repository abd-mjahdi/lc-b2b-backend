package com.lesieurcristal.b2bportal.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActivateAccountRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 8, max = 128) String password
) {
}
