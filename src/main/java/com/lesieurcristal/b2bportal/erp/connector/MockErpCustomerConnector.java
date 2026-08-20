package com.lesieurcristal.b2bportal.erp.connector;

import com.lesieurcristal.b2bportal.entity.erpmock.Customer;
import com.lesieurcristal.b2bportal.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MockErpCustomerConnector implements ErpCustomerConnector {

    private final CustomerRepository customerRepository;

    @Override
    public Optional<Customer> findByCustomerNumber(String customerNumber) {
        if (customerNumber == null || customerNumber.isBlank()) {
            return Optional.empty();
        }
        return customerRepository.findById(customerNumber);
    }
}
