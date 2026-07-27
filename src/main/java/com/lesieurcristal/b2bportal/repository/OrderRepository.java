package com.lesieurcristal.b2bportal.repository;

import com.lesieurcristal.b2bportal.entity.erpmock.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, String> {

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.invoice WHERE o.customer.customerNumber = :customerNumber ORDER BY o.orderDate DESC, o.orderNumber DESC")
    List<Order> findByCustomerCustomerNumberOrderByOrderDateDesc(@Param("customerNumber") String customerNumber);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.invoice WHERE o.orderNumber = :orderNumber")
    Optional<Order> findByOrderNumberWithInvoice(@Param("orderNumber") String orderNumber);
}
