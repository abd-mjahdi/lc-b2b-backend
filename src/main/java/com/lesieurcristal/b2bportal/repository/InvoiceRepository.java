package com.lesieurcristal.b2bportal.repository;

import com.lesieurcristal.b2bportal.entity.erpmock.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, String> {

    List<Invoice> findByCustomer_CustomerNumberOrderByInvoiceDateDesc(String customerNumber);

    @Query("SELECT i FROM Invoice i LEFT JOIN FETCH i.order LEFT JOIN FETCH i.customer "
            + "WHERE i.invoiceNumber = :invoiceNumber")
    Optional<Invoice> findByInvoiceNumberWithOrder(@Param("invoiceNumber") String invoiceNumber);
}
