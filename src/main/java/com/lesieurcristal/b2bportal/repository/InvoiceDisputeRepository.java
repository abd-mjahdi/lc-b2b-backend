package com.lesieurcristal.b2bportal.repository;

import com.lesieurcristal.b2bportal.entity.app.InvoiceDispute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceDisputeRepository extends JpaRepository<InvoiceDispute, Long> {

    @Query("SELECT d FROM InvoiceDispute d JOIN FETCH d.customer "
            + "WHERE d.status = :status ORDER BY d.createdAt DESC")
    List<InvoiceDispute> findByStatusOrderByCreatedAtDesc(
            @Param("status") InvoiceDispute.DisputeStatus status);

    @Query("SELECT d FROM InvoiceDispute d JOIN FETCH d.customer "
            + "WHERE d.customer.customerNumber = :customerNumber ORDER BY d.createdAt DESC")
    List<InvoiceDispute> findByCustomer_CustomerNumberOrderByCreatedAtDesc(
            @Param("customerNumber") String customerNumber);

    List<InvoiceDispute> findByInvoiceNumberOrderByCreatedAtDesc(String invoiceNumber);

    @Query("SELECT d FROM InvoiceDispute d JOIN FETCH d.customer ORDER BY d.createdAt DESC")
    @Override
    List<InvoiceDispute> findAll();

    long countByStatus(InvoiceDispute.DisputeStatus status);
}
