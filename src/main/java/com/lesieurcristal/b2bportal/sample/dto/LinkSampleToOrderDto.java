package com.lesieurcristal.b2bportal.sample.dto;

import jakarta.validation.constraints.NotBlank;

public record LinkSampleToOrderDto(
        @NotBlank String orderNumber
) {
}