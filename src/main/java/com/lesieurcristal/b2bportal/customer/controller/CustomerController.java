package com.lesieurcristal.b2bportal.customer.controller;

import com.lesieurcristal.b2bportal.customer.dto.ClientChangeLogDTO;
import com.lesieurcristal.b2bportal.customer.dto.CustomerProfileResponse;
import com.lesieurcristal.b2bportal.entity.app.ClientChangeLog;
import com.lesieurcristal.b2bportal.entity.erpmock.Customer;
import com.lesieurcristal.b2bportal.repository.ClientChangeLogRepository;
import com.lesieurcristal.b2bportal.repository.CustomerRepository;
import com.lesieurcristal.b2bportal.security.AuthenticatedUser;
import com.lesieurcristal.b2bportal.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerRepository customerRepository;
    private final ClientChangeLogRepository clientChangeLogRepository;

    @GetMapping("/me")
    public CustomerProfileResponse getMyProfile() {
        AuthenticatedUser user = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated"));

        String customerNumber = user.getCustomerNumber();
        if (customerNumber == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User does not have an associated customer number");
        }

        Customer customer = customerRepository.findById(customerNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));

        List<ClientChangeLog> logs = clientChangeLogRepository.findByCustomer_CustomerNumberOrderByChangedAtDesc(customerNumber);

        List<ClientChangeLogDTO> logDTOs = logs.stream().map(log -> ClientChangeLogDTO.builder()
                .id(log.getId())
                .fieldName(log.getFieldName())
                .oldValue(log.getOldValue())
                .newValue(log.getNewValue())
                .changedAt(log.getChangedAt())
                .changedByUser(log.getChangedByUser() != null ? log.getChangedByUser().getLogin() : "System")
                .build()).collect(Collectors.toList());

        return CustomerProfileResponse.builder()
                .customer(customer)
                .auditLog(logDTOs)
                .build();
    }
}
