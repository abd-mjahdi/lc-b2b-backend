package com.lesieurcristal.b2bportal.repository;

import com.lesieurcristal.b2bportal.entity.erpmock.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, String> {
}
