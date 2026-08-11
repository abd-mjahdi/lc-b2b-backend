package com.lesieurcristal.b2bportal.repository;

import com.lesieurcristal.b2bportal.entity.app.AppointmentRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppointmentRequestRepository extends JpaRepository<AppointmentRequest, Long> {
    List<AppointmentRequest> findByCustomer_CustomerNumberOrderByRequestedDateDesc(String customerNumber);
}
