package com.lesieurcristal.b2bportal.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerAdminResponse {
    private String customerNumber;
    private String companyName;
    private String postalAddress;
    private String city;
    private String country;
    private String phone;
    private String email;
    private String vatId;
    private long orderCount;
    private long invoiceCount;
    private long openReqCount;
    private double totalRevenue;
}
