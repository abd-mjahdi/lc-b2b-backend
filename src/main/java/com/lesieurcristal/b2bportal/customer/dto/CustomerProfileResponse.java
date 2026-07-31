package com.lesieurcristal.b2bportal.customer.dto;

import com.lesieurcristal.b2bportal.entity.erpmock.Customer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerProfileResponse {
    private Customer customer;
    private List<ClientChangeLogDTO> auditLog;
}
