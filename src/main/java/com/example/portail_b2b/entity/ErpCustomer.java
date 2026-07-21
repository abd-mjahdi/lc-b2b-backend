package com.example.portail_b2b.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "customers", schema = "erp_mock")
public class ErpCustomer {

    @Id
    @Column(name = "customer_number", length = 20)
    private String customerNumber;

    @Column(name = "company_name", nullable = false, length = 150)
    private String companyName;
}
