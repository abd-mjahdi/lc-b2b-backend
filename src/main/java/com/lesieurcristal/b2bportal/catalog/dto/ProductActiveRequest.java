package com.lesieurcristal.b2bportal.catalog.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public record ProductActiveRequest(
        @NotNull @JsonProperty("isActive") @JsonAlias("active") Boolean isActive
) {
}
