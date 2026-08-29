package com.lesieurcristal.b2bportal.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lesieurcristal.b2bportal.catalog.dto.ProductActiveRequest;
import com.lesieurcristal.b2bportal.catalog.dto.ProductDto;
import com.lesieurcristal.b2bportal.catalog.dto.UpsertProductRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ProductDtoJacksonTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void serializesBooleanFlagsWithIsPrefix() throws Exception {
        ProductDto dto = new ProductDto(
                "HSO-001",
                "Huile",
                "Huiles",
                "d",
                true,
                BigDecimal.ONE,
                BigDecimal.ZERO,
                "CAR",
                null,
                false,
                true
        );
        String json = mapper.writeValueAsString(dto);
        assertThat(json).contains("\"isActive\":false");
        assertThat(json).contains("\"isSampleable\":true");
        assertThat(json).contains("\"inStock\":true");
        assertThat(json).doesNotContain("\"active\":");
        assertThat(json).doesNotContain("\"sampleable\":");
    }

    @Test
    void deserializesProductActiveRequestFromIsActive() throws Exception {
        ProductActiveRequest body = mapper.readValue("{\"isActive\":false}", ProductActiveRequest.class);
        assertThat(body.isActive()).isFalse();
    }

    @Test
    void deserializesProductActiveRequestFromActiveAlias() throws Exception {
        ProductActiveRequest body = mapper.readValue("{\"active\":true}", ProductActiveRequest.class);
        assertThat(body.isActive()).isTrue();
    }

    @Test
    void deserializesUpsertFlagsFromIsPrefixedNames() throws Exception {
        UpsertProductRequest body = mapper.readValue(
                "{\"name\":\"Huile\",\"isSampleable\":true,\"isActive\":false,\"inStock\":true}",
                UpsertProductRequest.class
        );
        assertThat(body.isSampleable()).isTrue();
        assertThat(body.isActive()).isFalse();
        assertThat(body.inStock()).isTrue();
    }
}
