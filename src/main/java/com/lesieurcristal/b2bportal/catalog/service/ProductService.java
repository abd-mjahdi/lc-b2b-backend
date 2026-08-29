package com.lesieurcristal.b2bportal.catalog.service;

import com.lesieurcristal.b2bportal.catalog.dto.ProductDto;
import com.lesieurcristal.b2bportal.catalog.dto.UpsertProductRequest;
import com.lesieurcristal.b2bportal.entity.app.Product;
import com.lesieurcristal.b2bportal.repository.ProductRepository;
import com.lesieurcristal.b2bportal.repository.SampleRequestRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]{0,29}$");

    private final ProductRepository productRepository;
    private final SampleRequestRepository sampleRequestRepository;

    @Transactional(readOnly = true)
    public List<ProductDto> listCatalog() {
        return productRepository.findByIsActiveTrueOrderByCategoryAscNameAsc().stream()
                .map(ProductDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductDto> listSampleable() {
        return productRepository.findByIsActiveTrueAndIsSampleableTrueOrderByCategoryAscNameAsc()
                .stream()
                .filter(Product::isSellable)
                .map(ProductDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductDto> listAllForAdmin() {
        return productRepository.findAllByOrderByCategoryAscNameAsc().stream()
                .map(ProductDto::from)
                .toList();
    }

    @Transactional
    public ProductDto create(UpsertProductRequest request) {
        String code = normalizeCode(request.code());
        if (productRepository.existsById(code)) {
            throw new IllegalStateException("Un produit avec le code « " + code + " » existe déjà.");
        }
        Product saved = productRepository.save(apply(Product.builder().code(code).build(), request, true));
        return ProductDto.from(saved);
    }

    @Transactional
    public ProductDto update(String code, UpsertProductRequest request) {
        Product product = requireProduct(code);
        Product saved = productRepository.save(apply(product, request, false));
        return ProductDto.from(saved);
    }

    @Transactional
    public ProductDto setInStock(String code, boolean inStock) {
        Product product = requireProduct(code);
        product.setInStock(inStock);
        return ProductDto.from(productRepository.save(product));
    }

    @Transactional
    public ProductDto setActive(String code, boolean isActive) {
        Product product = requireProduct(code);
        product.setIsActive(isActive);
        return ProductDto.from(productRepository.save(product));
    }

    @Transactional
    public void delete(String code) {
        Product product = requireProduct(code);
        if (sampleRequestRepository.existsByProduct_Code(product.getCode())
                || productRepository.countQuotationRequestsByProductCode(product.getCode()) > 0
                || productRepository.countCertificatesByProductCode(product.getCode()) > 0) {
            throw new IllegalStateException(
                    "Ce produit est déjà utilisé (échantillons, cotations ou certificats). "
                            + "Retirez-le du catalogue plutôt que de le supprimer.");
        }
        try {
            productRepository.delete(product);
            productRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException(
                    "Ce produit est déjà utilisé. Retirez-le du catalogue plutôt que de le supprimer.");
        }
    }

    private Product requireProduct(String code) {
        String normalized = normalizeCode(code);
        return productRepository.findById(normalized)
                .or(() -> productRepository.findById(code))
                .orElseThrow(() -> new EntityNotFoundException("Produit introuvable : " + code));
    }

    private static Product apply(Product product, UpsertProductRequest request, boolean creating) {
        product.setName(trimRequired(request.name(), "Nom"));
        product.setCategory(blankToNull(request.category()));
        product.setDescription(blankToNull(request.description()));
        boolean sampleable = Boolean.TRUE.equals(request.isSampleable());
        product.setIsSampleable(sampleable);
        product.setMaxSampleQuantity(sampleable
                ? (request.maxSampleQuantity() != null ? request.maxSampleQuantity() : BigDecimal.ONE)
                : request.maxSampleQuantity());
        product.setUnitPrice(request.unitPrice());
        String unit = blankToNull(request.salesUnit());
        product.setSalesUnit(unit != null ? unit.toUpperCase(Locale.ROOT) : (creating ? "CAR" : product.getSalesUnit()));
        product.setImageUrl(normalizeImageUrl(request.imageUrl()));
        if (request.isActive() != null) {
            product.setIsActive(request.isActive());
        } else if (creating) {
            product.setIsActive(true);
        }
        if (request.inStock() != null) {
            product.setInStock(request.inStock());
        } else if (creating) {
            product.setInStock(true);
        }
        return product;
    }

    static String normalizeCode(String code) {
        String trimmed = code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Le code produit est obligatoire.");
        }
        if (!CODE_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException(
                    "Code produit invalide. Utilisez 1 à 30 caractères (lettres, chiffres, ., _ ou -).");
        }
        return trimmed;
    }

    private static String trimRequired(String value, String field) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(field + " obligatoire.");
        }
        return trimmed;
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String normalizeImageUrl(String value) {
        String trimmed = blankToNull(value);
        if (trimmed == null) {
            return null;
        }
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.startsWith("http://") || lower.startsWith("https://") || trimmed.startsWith("/")) {
            return trimmed;
        }
        throw new IllegalArgumentException("L'URL d'image doit commencer par https://, http:// ou /.");
    }
}
