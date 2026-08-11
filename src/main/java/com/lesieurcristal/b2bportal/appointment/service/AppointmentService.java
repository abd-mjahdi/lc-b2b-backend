package com.lesieurcristal.b2bportal.appointment.service;

import com.lesieurcristal.b2bportal.appointment.dto.AppointmentResponseDto;
import com.lesieurcristal.b2bportal.appointment.dto.CreateAppointmentRequest;
import com.lesieurcristal.b2bportal.entity.app.AppointmentRequest;
import com.lesieurcristal.b2bportal.entity.app.User;
import com.lesieurcristal.b2bportal.entity.erpmock.Customer;
import com.lesieurcristal.b2bportal.notification.service.PortalNotificationService;
import com.lesieurcristal.b2bportal.repository.AppointmentRequestRepository;
import com.lesieurcristal.b2bportal.repository.CustomerRepository;
import com.lesieurcristal.b2bportal.repository.UserRepository;
import com.lesieurcristal.b2bportal.security.AuthenticatedUser;
import com.lesieurcristal.b2bportal.security.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final AppointmentRequestRepository appointmentRepository;
    private final PortalNotificationService portalNotificationService;

    @Transactional
    public AppointmentResponseDto create(CreateAppointmentRequest dto) {
        AuthenticatedUser current = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Non authentifié"));
        if (current.getCustomerNumber() == null) {
            throw new SecurityException("Seuls les clients peuvent réserver un rendez-vous.");
        }
        Customer customer = customerRepository.findById(current.getCustomerNumber())
                .orElseThrow(() -> new EntityNotFoundException("Client inconnu"));
        User user = userRepository.findById(current.getId()).orElse(null);

        AppointmentRequest persisted = appointmentRepository.save(AppointmentRequest.builder()
                .customer(customer)
                .user(user)
                .subject(dto.subject())
                .requestedDate(dto.requestedDate())
                .requestedTimeSlot(dto.requestedTimeSlot())
                .status("pending")
                .build());

        portalNotificationService.createAdminBroadcast(
                "Nouvelle demande de rendez-vous",
                String.format("%s (Client #%s) souhaite un RDV le %s (%s). Objet : %s.",
                        customer.getCompanyName(),
                        customer.getCustomerNumber(),
                        dto.requestedDate(),
                        dto.requestedTimeSlot(),
                        dto.subject() != null ? dto.subject() : "Non précisé"),
                "APPOINTMENT_REQUESTED",
                "APPOINTMENT",
                persisted.getId().toString(),
                "/admin/appointments"
        );
        return AppointmentResponseDto.from(persisted);
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponseDto> listForCurrentUser() {
        AuthenticatedUser current = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Non authentifié"));
        if (current.getCustomerNumber() == null) {
            return List.of();
        }
        return appointmentRepository
                .findByCustomer_CustomerNumberOrderByRequestedDateDesc(current.getCustomerNumber())
                .stream()
                .map(AppointmentResponseDto::from)
                .toList();
    }
}
