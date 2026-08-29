package com.lesieurcristal.b2bportal.catalog;

import com.lesieurcristal.b2bportal.catalog.dto.ProductDto;
import com.lesieurcristal.b2bportal.catalog.dto.UpsertProductRequest;
import com.lesieurcristal.b2bportal.catalog.service.ProductService;
import com.lesieurcristal.b2bportal.entity.app.Product;
import com.lesieurcristal.b2bportal.repository.ProductRepository;
import com.lesieurcristal.b2bportal.repository.SampleRequestRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private SampleRequestRepository sampleRequestRepository;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository, sampleRequestRepository);
    }

    @Test
    void create_normalizesCodeAndDefaultsStock() {
        when(productRepository.existsById("HSO-009")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductDto dto = productService.create(new UpsertProductRequest(
                "hso-009",
                " Huile test ",
                "Huiles",
                "Desc",
                true,
                new BigDecimal("2"),
                new BigDecimal("100.00"),
                "car",
                "https://example.com/p.png",
                null,
                null
        ));

        assertThat(dto.code()).isEqualTo("HSO-009");
        assertThat(dto.name()).isEqualTo("Huile test");
        assertThat(dto.salesUnit()).isEqualTo("CAR");
        assertThat(dto.isActive()).isTrue();
        assertThat(dto.inStock()).isTrue();
        assertThat(dto.isSampleable()).isTrue();
    }

    @Test
    void create_rejectsDuplicateCode() {
        when(productRepository.existsById("HSO-001")).thenReturn(true);

        assertThatThrownBy(() -> productService.create(new UpsertProductRequest(
                "HSO-001", "x", null, null, false, null, null, null, null, true, true
        ))).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("existe déjà");
        verify(productRepository, never()).save(any());
    }

    @Test
    void update_changesFieldsAndKeepsCode() {
        Product existing = Product.builder()
                .code("HSO-001")
                .name("Old")
                .isActive(true)
                .inStock(true)
                .isSampleable(false)
                .build();
        when(productRepository.findById("HSO-001")).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductDto dto = productService.update("HSO-001", new UpsertProductRequest(
                "ignored",
                "Huile de soja",
                "Huiles",
                "Maj",
                true,
                new BigDecimal("1"),
                new BigDecimal("290"),
                "CAR",
                null,
                true,
                false
        ));

        assertThat(dto.code()).isEqualTo("HSO-001");
        assertThat(dto.name()).isEqualTo("Huile de soja");
        assertThat(dto.inStock()).isFalse();
        assertThat(dto.isSampleable()).isTrue();
    }

    @Test
    void setInStock_togglesAvailability() {
        Product existing = Product.builder()
                .code("HSO-001")
                .name("Huile")
                .isActive(true)
                .inStock(true)
                .build();
        when(productRepository.findById("HSO-001")).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductDto dto = productService.setInStock("HSO-001", false);

        assertThat(dto.inStock()).isFalse();
        assertThat(dto.isActive()).isTrue();
    }

    @Test
    void delete_removesUnusedProduct() {
        Product existing = Product.builder().code("NEW-001").name("X").isActive(true).inStock(true).build();
        when(productRepository.findById("NEW-001")).thenReturn(Optional.of(existing));
        when(sampleRequestRepository.existsByProduct_Code("NEW-001")).thenReturn(false);
        when(productRepository.countQuotationRequestsByProductCode("NEW-001")).thenReturn(0L);
        when(productRepository.countCertificatesByProductCode("NEW-001")).thenReturn(0L);

        productService.delete("NEW-001");

        verify(productRepository).delete(existing);
        verify(productRepository).flush();
    }

    @Test
    void delete_rejectsWhenReferenced() {
        Product existing = Product.builder().code("HSO-001").name("X").isActive(true).inStock(true).build();
        when(productRepository.findById("HSO-001")).thenReturn(Optional.of(existing));
        when(sampleRequestRepository.existsByProduct_Code("HSO-001")).thenReturn(true);

        assertThatThrownBy(() -> productService.delete("HSO-001"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Retirez-le");
        verify(productRepository, never()).delete(any());
    }

    @Test
    void listCatalog_excludesWithdrawn() {
        Product listed = Product.builder().code("A").name("A").category("H").isActive(true).inStock(false).build();
        when(productRepository.findByIsActiveTrueOrderByCategoryAscNameAsc()).thenReturn(List.of(listed));

        List<ProductDto> rows = productService.listCatalog();

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).inStock()).isFalse();
    }

    @Test
    void update_unknownProduct() {
        when(productRepository.findById("NOPE")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> productService.update("NOPE", new UpsertProductRequest(
                "NOPE", "x", null, null, false, null, null, null, null, true, true
        ))).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void create_persistsNormalizedCode() {
        when(productRepository.existsById("ABC-1")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        productService.create(new UpsertProductRequest(
                "abc-1", "Name", null, null, false, null, BigDecimal.TEN, null, null, true, true
        ));

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo("ABC-1");
        assertThat(captor.getValue().getUnitPrice()).isEqualByComparingTo("10");
    }
}
