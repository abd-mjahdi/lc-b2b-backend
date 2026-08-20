package com.lesieurcristal.b2bportal.erp.connector;

import com.lesieurcristal.b2bportal.entity.erpmock.Invoice;

import java.util.List;
import java.util.Optional;

/** Door to ERP invoices. */
public interface ErpInvoiceConnector {

    List<Invoice> findByCustomerNumber(String customerNumber);

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
}
