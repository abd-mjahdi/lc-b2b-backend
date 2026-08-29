package com.lesieurcristal.b2bportal.catalog.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lesieurcristal.b2bportal.entity.app.Product;

import java.math.BigDecimal;

public record ProductDto(
        String code,
        String name,
        String category,
        String description,
        @JsonProperty("isSampleable") @JsonAlias("sampleable") Boolean isSampleable,
        BigDecimal maxSampleQuantity,
        BigDecimal unitPrice,
        String salesUnit,
        String imageUrl,
        @JsonProperty("isActive") @JsonAlias("active") Boolean isActive,
        Boolean inStock
) {
    public static ProductDto from(Product p) {
        return new ProductDto(
                p.getCode(),
                p.getName(),
                p.getCategory(),
                p.getDescription(),
                Boolean.TRUE.equals(p.getIsSampleable()),
                p.getMaxSampleQuantity(),
                p.getUnitPrice(),
                p.getSalesUnit(),
                p.getImageUrl(),
                p.isListed(),
                !Boolean.FALSE.equals(p.getInStock())
        );
    }
}
