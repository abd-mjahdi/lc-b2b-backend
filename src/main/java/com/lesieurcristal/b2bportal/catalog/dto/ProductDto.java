package com.lesieurcristal.b2bportal.catalog.dto;

import com.lesieurcristal.b2bportal.entity.app.Product;

import java.math.BigDecimal;

public record ProductDto(
        String code,
        String name,
        String category,
        String description,
        Boolean isSampleable,
        BigDecimal maxSampleQuantity,
        BigDecimal unitPrice,
        String salesUnit,
        String imageUrl
) {
    public static ProductDto from(Product p) {
        return new ProductDto(
                p.getCode(), p.getName(), p.getCategory(), p.getDescription(),
                p.getIsSampleable(), p.getMaxSampleQuantity(), p.getUnitPrice(),
                p.getSalesUnit(), p.getImageUrl()
        );
    }
}