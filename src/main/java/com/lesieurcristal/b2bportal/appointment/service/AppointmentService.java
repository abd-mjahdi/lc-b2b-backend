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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;

@Slf4j
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
                "/admin/rendez-vous"
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

    @Transactional(readOnly = true)
    public List<AppointmentResponseDto> listAllForAdmin(String customerNumber) {
        List<AppointmentRequest> rows;
        if (customerNumber != null && !customerNumber.isBlank()) {
            rows = appointmentRepository
                    .findByCustomer_CustomerNumberOrderByRequestedDateDesc(customerNumber.trim());
        } else {
            rows = appointmentRepository.findAll();
            rows.sort(Comparator
                    .comparing(AppointmentRequest::getRequestedDate,
                            Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(AppointmentRequest::getId,
                            Comparator.nullsLast(Comparator.reverseOrder())));
        }
        return rows.stream().map(AppointmentResponseDto::from).toList();
    }

    @Transactional
    public AppointmentResponseDto confirm(Long id) {
        AppointmentRequest apt = requirePending(id);
        apt.setStatus("confirmed");
        apt.setConfirmedAt(OffsetDateTime.now());
        AppointmentRequest saved = appointmentRepository.save(apt);

        notifyClient(
                saved,
                "Votre rendez-vous est confirmé",
                String.format(
                        "Votre rendez-vous du %s (%s) a été confirmé. Objet : %s.",
                        saved.getRequestedDate(),
                        saved.getRequestedTimeSlot(),
                        saved.getSubject() != null ? saved.getSubject() : "—")
        );
        return AppointmentResponseDto.from(saved);
    }

    @Transactional
    public AppointmentResponseDto cancel(Long id) {
        AppointmentRequest apt = requirePending(id);
        apt.setStatus("cancelled");
        AppointmentRequest saved = appointmentRepository.save(apt);

        notifyClient(
                saved,
                "Rendez-vous annulé",
                String.format(
                        "Votre demande de rendez-vous du %s (%s) a été annulée. Objet : %s.",
                        saved.getRequestedDate(),
                        saved.getRequestedTimeSlot(),
                        saved.getSubject() != null ? saved.getSubject() : "—")
        );
        return AppointmentResponseDto.from(saved);
    }

    private AppointmentRequest requirePending(Long id) {
        AppointmentRequest apt = appointmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Rendez-vous introuvable"));
        if (!"pending".equals(apt.getStatus())) {
            throw new IllegalStateException(
                    "Seules les demandes en attente (pending) peuvent être tranchées. Statut actuel : "
                            + apt.getStatus());
        }
        return apt;
    }

    private void notifyClient(AppointmentRequest apt, String title, String message) {
        if (apt.getUser() != null) {
            portalNotificationService.createNotificationForUser(
                    apt.getUser(),
                    title,
                    message,
                    "APPOINTMENT_STATUS_CHANGED",
                    "APPOINTMENT",
                    String.valueOf(apt.getId()),
                    "/dashboard/rendez-vous"
            );
            return;
        }
        if (apt.getCustomer() == null) {
            log.warn("Appointment {} updated but no user/customer to notify", apt.getId());
            return;
        }
        for (User recipient : userRepository.findByCustomer_CustomerNumber(
                apt.getCustomer().getCustomerNumber())) {
            portalNotificationService.createNotificationForUser(
                    recipient,
                    title,
                    message,
                    "APPOINTMENT_STATUS_CHANGED",
                    "APPOINTMENT",
                    String.valueOf(apt.getId()),
                    "/dashboard/rendez-vous"
            );
        }
    }
}
