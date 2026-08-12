package com.lesieurcristal.b2bportal.invoice.pdf;

import com.lesieurcristal.b2bportal.entity.erpmock.Customer;
import com.lesieurcristal.b2bportal.entity.erpmock.Invoice;
import com.lesieurcristal.b2bportal.entity.erpmock.Order;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class InvoicePdfTemplateTest {

    private final InvoicePdfTemplate template = new InvoicePdfTemplate();

    @Test
    void generate_producesValidPdfBytes() {
        Customer customer = Customer.builder()
                .customerNumber("CUST0001")
                .companyName("Épicerie Al Amal SARL")
                .postalAddress("12 Rue des Orangers")
                .city("Casablanca")
                .country("Maroc")
                .vatId("ICE001234567890")
                .build();

        Order order = Order.builder()
                .orderNumber("4500010001")
                .orderDate(LocalDate.of(2026, 5, 5))
                .customer(customer)
                .productCode("HTO-001")
                .productLabel("Huile de tournesol Lesieur 1L")
                .quantityOrdered(new BigDecimal("500"))
                .salesUnit("CAR")
                .netAmount(new BigDecimal("72000.00"))
                .currency("MAD")
                .build();

        Invoice invoice = Invoice.builder()
                .invoiceNumber("900010001")
                .invoiceDate(LocalDate.of(2026, 5, 13))
                .dueDate(LocalDate.of(2026, 6, 12))
                .paymentDate(LocalDate.of(2026, 6, 5))
                .customer(customer)
                .order(order)
                .netAmount(new BigDecimal("72000.00"))
                .vatAmount(new BigDecimal("7200.00"))
                .totalAmount(new BigDecimal("79200.00"))
                .currency("MAD")
                .invoiceStatus("paid")
                .build();

        byte[] pdf = template.generate(invoice);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
        assertThat(pdf.length).isGreaterThan(500);
    }
}
