package com.lesieurcristal.b2bportal.sample.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateSampleRequestDto(
        @NotBlank String productCode,
        @Positive BigDecimal quantity,
        @NotBlank String contactName,
        @NotBlank String contactAddress
) {
}