package com.lesieurcristal.b2bportal.catalog.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpsertProductRequest(
        @Size(max = 30) String code,
        @NotBlank @Size(max = 150) String name,
        @Size(max = 100) String category,
        String description,
        @JsonProperty("isSampleable") @JsonAlias("sampleable") Boolean isSampleable,
        @DecimalMin(value = "0.001", inclusive = true) BigDecimal maxSampleQuantity,
        @DecimalMin(value = "0.00", inclusive = true) BigDecimal unitPrice,
        @Size(max = 10) String salesUnit,
        @Size(max = 500) String imageUrl,
        @JsonProperty("isActive") @JsonAlias("active") Boolean isActive,
        Boolean inStock
) {
}
