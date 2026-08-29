package com.lesieurcristal.b2bportal.repository;

import com.lesieurcristal.b2bportal.entity.app.SampleRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SampleRequestRepository extends JpaRepository<SampleRequest, Long> {

    List<SampleRequest> findByCustomer_CustomerNumberOrderByRequestedAtDesc(String customerNumber);

    List<SampleRequest> findByStatusOrderByRequestedAtDesc(String status);

    @Query("SELECT s FROM SampleRequest s "
            + "LEFT JOIN FETCH s.customer "
            + "LEFT JOIN FETCH s.product "
            + "LEFT JOIN FETCH s.resultingOrder "
            + "LEFT JOIN FETCH s.user "
            + "ORDER BY s.requestedAt DESC")
    @Override
    List<SampleRequest> findAll();

    long countByStatus(String status);

    boolean existsByProduct_Code(String code);
}
