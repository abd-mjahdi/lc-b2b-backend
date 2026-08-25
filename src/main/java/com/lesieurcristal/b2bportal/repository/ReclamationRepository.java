package com.lesieurcristal.b2bportal.repository;

import com.lesieurcristal.b2bportal.entity.app.Reclamation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReclamationRepository extends JpaRepository<Reclamation, Long> {

    @Query("SELECT r FROM Reclamation r JOIN FETCH r.customer "
            + "WHERE r.customer.customerNumber = :customerNumber ORDER BY r.receivedAt DESC")
    List<Reclamation> findByCustomer_CustomerNumberOrderByReceivedAtDesc(
            @Param("customerNumber") String customerNumber);

    @Query("SELECT r FROM Reclamation r JOIN FETCH r.customer")
    @Override
    List<Reclamation> findAll();

    long countByStatus(String status);
}
