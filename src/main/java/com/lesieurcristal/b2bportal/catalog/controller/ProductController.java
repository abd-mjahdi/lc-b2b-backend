package com.lesieurcristal.b2bportal.catalog.controller;

import com.lesieurcristal.b2bportal.catalog.dto.ProductDto;
import com.lesieurcristal.b2bportal.repository.ProductRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Catalogue produits", description = "Consultation du catalogue Lesieur Cristal")
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ProductController {

    private final ProductRepository productRepository;

    @Operation(summary = "Liste complète du catalogue, triée par catégorie puis nom")
    @GetMapping
    public ResponseEntity<List<ProductDto>> listAll() {
        return ResponseEntity.ok(productRepository
                .findByIsActiveTrueOrderByCategoryAscNameAsc()
                .stream().map(ProductDto::from).toList());
    }

    @Operation(summary = "Liste des produits éligibles aux échantillons")
    @GetMapping("/sampleable")
    public ResponseEntity<List<ProductDto>> listSampleable() {
        return ResponseEntity.ok(productRepository
                .findByIsActiveTrueAndIsSampleableTrueOrderByCategoryAscNameAsc()
                .stream().map(ProductDto::from).toList());
    }
}