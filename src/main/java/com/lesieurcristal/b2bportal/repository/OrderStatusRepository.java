package com.lesieurcristal.b2bportal.repository;

import com.lesieurcristal.b2bportal.entity.erpmock.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderStatusRepository extends JpaRepository<OrderStatus, String> {

    Optional<OrderStatus> findByOrderNumber(String orderNumber);
}
