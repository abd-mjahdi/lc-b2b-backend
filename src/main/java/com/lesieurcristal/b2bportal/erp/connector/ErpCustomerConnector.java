package com.lesieurcristal.b2bportal.erp.connector;

import com.lesieurcristal.b2bportal.entity.erpmock.Customer;

import java.util.Optional;

/** Door to ERP customer master. */
public interface ErpCustomerConnector {

    Optional<Customer> findByCustomerNumber(String customerNumber);
}
