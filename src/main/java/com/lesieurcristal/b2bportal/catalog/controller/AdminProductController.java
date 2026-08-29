package com.lesieurcristal.b2bportal.catalog.controller;

import com.lesieurcristal.b2bportal.catalog.dto.ProductActiveRequest;
import com.lesieurcristal.b2bportal.catalog.dto.ProductDto;
import com.lesieurcristal.b2bportal.catalog.dto.ProductStockRequest;
import com.lesieurcristal.b2bportal.catalog.dto.UpsertProductRequest;
import com.lesieurcristal.b2bportal.catalog.service.ProductService;
import com.lesieurcristal.b2bportal.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Admin — Catalogue", description = "Création, modification, rupture de stock et suppression des produits")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductController {

    private final ProductService productService;

    @Operation(summary = "Liste de tous les produits (actifs et retirés)")
    @GetMapping
    public ResponseEntity<List<ProductDto>> list() {
        return ResponseEntity.ok(productService.listAllForAdmin());
    }

    @Operation(summary = "Ajoute un produit au catalogue")
    @PostMapping
    public ResponseEntity<ProductDto> create(@Valid @RequestBody UpsertProductRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(body));
    }

    @Operation(summary = "Modifie un produit existant")
    @PutMapping("/{code}")
    public ResponseEntity<ProductDto> update(
            @PathVariable String code,
            @Valid @RequestBody UpsertProductRequest body) {
        return ResponseEntity.ok(productService.update(code, body));
    }

    @Operation(summary = "Marque le produit en stock ou en rupture")
    @PutMapping("/{code}/stock")
    public ResponseEntity<ProductDto> setStock(
            @PathVariable String code,
            @Valid @RequestBody ProductStockRequest body) {
        return ResponseEntity.ok(productService.setInStock(code, body.inStock()));
    }

    @Operation(summary = "Retire ou réintègre le produit dans le catalogue client")
    @PutMapping("/{code}/active")
    public ResponseEntity<ProductDto> setActive(
            @PathVariable String code,
            @Valid @RequestBody ProductActiveRequest body) {
        return ResponseEntity.ok(productService.setActive(code, body.isActive()));
    }

    @Operation(summary = "Supprime un produit inutilisé. S'il a un historique, le retirer du catalogue à la place.")
    @DeleteMapping("/{code}")
    public ResponseEntity<Void> delete(@PathVariable String code) {
        productService.delete(code);
        return ResponseEntity.noContent().build();
    }
}
