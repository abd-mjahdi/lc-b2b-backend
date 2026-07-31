package com.lesieurcristal.b2bportal.repository;

import com.lesieurcristal.b2bportal.entity.app.ClientChangeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ClientChangeLogRepository extends JpaRepository<ClientChangeLog, Long> {
    List<ClientChangeLog> findByCustomer_CustomerNumberOrderByChangedAtDesc(String customerNumber);
}
