package com.lesieurcristal.b2bportal.invoice.service;

import com.lesieurcristal.b2bportal.entity.app.Document;
import com.lesieurcristal.b2bportal.entity.app.User;
import com.lesieurcristal.b2bportal.entity.app.enums.UserRole;
import com.lesieurcristal.b2bportal.entity.erpmock.Customer;
import com.lesieurcristal.b2bportal.entity.erpmock.Invoice;
import com.lesieurcristal.b2bportal.entity.erpmock.Order;
import com.lesieurcristal.b2bportal.invoice.pdf.InvoicePdfTemplate;
import com.lesieurcristal.b2bportal.repository.DocumentRepository;
import com.lesieurcristal.b2bportal.repository.InvoiceRepository;
import com.lesieurcristal.b2bportal.security.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceDocumentServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private InvoicePdfTemplate invoicePdfTemplate;

    @InjectMocks
    private InvoiceDocumentService service;

    @TempDir
    Path tempDir;

    private Customer customer;
    private Invoice invoice;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "storageDir", tempDir.toString());

        customer = Customer.builder()
                .customerNumber("CUST0001")
                .companyName("Épicerie Al Amal SARL")
                .build();

        Order order = Order.builder()
                .orderNumber("4500010001")
                .productCode("HTO-001")
                .productLabel("Huile de tournesol Lesieur 1L")
                .quantityOrdered(new BigDecimal("500"))
                .salesUnit("CAR")
                .netAmount(new BigDecimal("72000.00"))
                .currency("MAD")
                .build();

        invoice = Invoice.builder()
                .invoiceNumber("900010001")
                .invoiceDate(LocalDate.of(2026, 5, 13))
                .customer(customer)
                .order(order)
                .netAmount(new BigDecimal("72000.00"))
                .vatAmount(new BigDecimal("7200.00"))
                .totalAmount(new BigDecimal("79200.00"))
                .currency("MAD")
                .invoiceStatus("paid")
                .build();

        authenticateAs("CUST0001");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String customerNumber) {
        Customer c = Customer.builder().customerNumber(customerNumber).build();
        User user = User.builder()
                .id(1L)
                .login("aalami")
                .passwordHash("x")
                .role(UserRole.CLIENT)
                .customer(c)
                .isActive(true)
                .build();
        AuthenticatedUser auth = new AuthenticatedUser(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(auth, null, auth.getAuthorities()));
    }

    @Test
    void download_generatesOnceAndPersistsDocument() throws Exception {
        when(invoiceRepository.findByInvoiceNumberWithOrder("900010001")).thenReturn(Optional.of(invoice));
        when(documentRepository.findByDocTypeAndNaturalKey("invoice", "900010001")).thenReturn(Optional.empty());
        byte[] pdf = "%PDF-fake-content".getBytes(StandardCharsets.US_ASCII);
        when(invoicePdfTemplate.generate(invoice)).thenReturn(pdf);
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> {
            Document d = inv.getArgument(0);
            d.setId(42L);
            return d;
        });

        InvoiceDocumentService.InvoiceFile first = service.downloadInvoicePdf("900010001");

        assertThat(first.content()).isEqualTo(pdf);
        assertThat(Files.exists(tempDir.resolve("invoices/900010001.pdf"))).isTrue();
        verify(invoicePdfTemplate, times(1)).generate(invoice);
        verify(documentRepository).save(any(Document.class));
    }

    @Test
    void download_reusesExistingFileWithoutRegenerating() throws Exception {
        Path existingFile = tempDir.resolve("invoices/900010001.pdf");
        Files.createDirectories(existingFile.getParent());
        byte[] cached = "%PDF-cached".getBytes(StandardCharsets.US_ASCII);
        Files.write(existingFile, cached);

        Document doc = Document.builder()
                .id(7L)
                .customer(customer)
                .docType("invoice")
                .naturalKey("900010001")
                .status("ready")
                .filePath("/documents/invoices/900010001.pdf")
                .build();

        when(invoiceRepository.findByInvoiceNumberWithOrder("900010001")).thenReturn(Optional.of(invoice));
        when(documentRepository.findByDocTypeAndNaturalKey("invoice", "900010001")).thenReturn(Optional.of(doc));

        InvoiceDocumentService.InvoiceFile file = service.downloadInvoicePdf("900010001");

        assertThat(file.content()).isEqualTo(cached);
        verify(invoicePdfTemplate, never()).generate(any());
        verify(documentRepository, never()).save(any());
    }

    @Test
    void download_hidesOtherCustomersInvoices() {
        authenticateAs("CUST0002");
        when(invoiceRepository.findByInvoiceNumberWithOrder("900010001")).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> service.downloadInvoicePdf("900010001"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");

        verify(invoicePdfTemplate, never()).generate(any());
        verify(documentRepository, never()).findByDocTypeAndNaturalKey(eq("invoice"), any());
    }
}
