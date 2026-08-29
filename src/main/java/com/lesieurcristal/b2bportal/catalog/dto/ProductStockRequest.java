package com.lesieurcristal.b2bportal.catalog.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public record ProductStockRequest(
        @NotNull @JsonProperty("inStock") Boolean inStock
) {
}
