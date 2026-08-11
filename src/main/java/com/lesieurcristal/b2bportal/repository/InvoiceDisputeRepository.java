package com.lesieurcristal.b2bportal.repository;

import com.lesieurcristal.b2bportal.entity.app.InvoiceDispute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceDisputeRepository extends JpaRepository<InvoiceDispute, Long> {

    List<InvoiceDispute> findByStatusOrderByCreatedAtDesc(InvoiceDispute.DisputeStatus status);

    List<InvoiceDispute> findByCustomer_CustomerNumberOrderByCreatedAtDesc(String customerNumber);

    List<InvoiceDispute> findByInvoiceNumberOrderByCreatedAtDesc(String invoiceNumber);

    long countByStatus(InvoiceDispute.DisputeStatus status);
}
