package com.lesieurcristal.b2bportal.claim.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateReclamationRequest(
        @NotBlank @Size(max = 50) String lotNumber,
        @NotBlank @Size(max = 4000) String description,
        @Size(max = 500) String attachmentPath
) {
}
