package com.lesieurcristal.b2bportal.sample.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateSampleStatusDto(
        @NotBlank
        @Pattern(regexp = "new|processing|fulfilled|rejected",
                message = "Statut autorisé : new, processing, fulfilled, rejected")
        String status
) {
}