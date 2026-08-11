package com.lesieurcristal.b2bportal.repository;

import com.lesieurcristal.b2bportal.entity.app.SampleRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SampleRequestRepository extends JpaRepository<SampleRequest, Long> {

    List<SampleRequest> findByCustomer_CustomerNumberOrderByRequestedAtDesc(String customerNumber);

    List<SampleRequest> findByStatusOrderByRequestedAtDesc(String status);

    long countByStatus(String status);
}
