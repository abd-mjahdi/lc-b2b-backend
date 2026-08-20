package com.lesieurcristal.b2bportal.erp.connector;

import com.lesieurcristal.b2bportal.entity.erpmock.Invoice;
import com.lesieurcristal.b2bportal.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MockErpInvoiceConnector implements ErpInvoiceConnector {

    private final InvoiceRepository invoiceRepository;

    @Override
    public List<Invoice> findByCustomerNumber(String customerNumber) {
        if (customerNumber == null || customerNumber.isBlank()) {
            return List.of();
        }
        return invoiceRepository.findByCustomer_CustomerNumberWithOrderOrderByInvoiceDateDesc(customerNumber);
    }

    @Override
    public Optional<Invoice> findByInvoiceNumber(String invoiceNumber) {
        if (invoiceNumber == null || invoiceNumber.isBlank()) {
            return Optional.empty();
        }
        return invoiceRepository.findByInvoiceNumberWithOrder(invoiceNumber);
    }
}
