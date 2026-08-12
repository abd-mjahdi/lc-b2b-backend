package com.lesieurcristal.b2bportal.customer.service;

import com.lesieurcristal.b2bportal.customer.dto.ClientChangeLogDTO;
import com.lesieurcristal.b2bportal.customer.dto.CustomerProfileResponse;
import com.lesieurcristal.b2bportal.customer.dto.UpdateCustomerProfileRequest;
import com.lesieurcristal.b2bportal.entity.app.ClientChangeLog;
import com.lesieurcristal.b2bportal.entity.app.User;
import com.lesieurcristal.b2bportal.entity.erpmock.Customer;
import com.lesieurcristal.b2bportal.repository.ClientChangeLogRepository;
import com.lesieurcristal.b2bportal.repository.CustomerRepository;
import com.lesieurcristal.b2bportal.repository.UserRepository;
import com.lesieurcristal.b2bportal.security.AuthenticatedUser;
import com.lesieurcristal.b2bportal.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final ClientChangeLogRepository clientChangeLogRepository;
    private final UserRepository userRepository;

    public CustomerProfileResponse getMyProfile() {
        Customer customer = loadCurrentCustomer();
        return toProfileResponse(customer);
    }

    /**
     * Met à jour les champs autorisés ({@code phone}, {@code email}) et
     * enregistre chaque modification dans {@code app.client_change_log}.
     */
    @Transactional
    public CustomerProfileResponse updateMyProfile(UpdateCustomerProfileRequest request) {
        AuthenticatedUser current = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated"));

        Customer customer = loadCurrentCustomer();
        User changedBy = userRepository.findById(current.getId()).orElse(null);

        List<ClientChangeLog> changes = new ArrayList<>();

        applyIfPresent(request.phone(), customer.getPhone(), "phone", customer::setPhone, customer, changedBy, changes);
        applyIfPresent(request.email(), customer.getEmail(), "email", customer::setEmail, customer, changedBy, changes);

        if (!changes.isEmpty()) {
            customerRepository.save(customer);
            clientChangeLogRepository.saveAll(changes);
            log.info("Profil client {} mis à jour ({} champ(s)) par user={}",
                    customer.getCustomerNumber(), changes.size(), current.getId());
        }

        return toProfileResponse(customer);
    }

    private Customer loadCurrentCustomer() {
        AuthenticatedUser user = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated"));

        String customerNumber = user.getCustomerNumber();
        if (customerNumber == null || customerNumber.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "User does not have an associated customer number");
        }

        return customerRepository.findById(customerNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));
    }

    private void applyIfPresent(
            String newValue,
            String oldValue,
            String fieldName,
            Consumer<String> setter,
            Customer customer,
            User changedBy,
            List<ClientChangeLog> changes) {

        if (newValue == null) {
            return;
        }
        String normalizedNew = newValue.isBlank() ? null : newValue.trim();
        String normalizedOld = (oldValue == null || oldValue.isBlank()) ? null : oldValue;

        if (Objects.equals(normalizedOld, normalizedNew)) {
            return;
        }

        setter.accept(normalizedNew);
        changes.add(ClientChangeLog.builder()
                .customer(customer)
                .changedByUser(changedBy)
                .fieldName(fieldName)
                .oldValue(normalizedOld)
                .newValue(normalizedNew)
                .build());
    }

    private CustomerProfileResponse toProfileResponse(Customer customer) {
        List<ClientChangeLogDTO> logDTOs = clientChangeLogRepository
                .findByCustomer_CustomerNumberOrderByChangedAtDesc(customer.getCustomerNumber())
                .stream()
                .map(logEntry -> ClientChangeLogDTO.builder()
                        .id(logEntry.getId())
                        .fieldName(logEntry.getFieldName())
                        .oldValue(logEntry.getOldValue())
                        .newValue(logEntry.getNewValue())
                        .changedAt(logEntry.getChangedAt())
                        .changedByUser(logEntry.getChangedByUser() != null
                                ? logEntry.getChangedByUser().getLogin()
                                : "System")
                        .build())
                .toList();

        return CustomerProfileResponse.builder()
                .customer(customer)
                .auditLog(logDTOs)
                .build();
    }
}
