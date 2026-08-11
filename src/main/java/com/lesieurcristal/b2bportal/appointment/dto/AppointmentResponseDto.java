package com.lesieurcristal.b2bportal.appointment.dto;

import com.lesieurcristal.b2bportal.entity.app.AppointmentRequest;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record AppointmentResponseDto(
        Long id,
        String customerNumber,
        Long userId,
        String subject,
        LocalDate requestedDate,
        String requestedTimeSlot,
        String status,
        OffsetDateTime confirmedAt,
        OffsetDateTime createdAt
) {
    public static AppointmentResponseDto from(AppointmentRequest a) {
        return new AppointmentResponseDto(
                a.getId(),
                a.getCustomer() != null ? a.getCustomer().getCustomerNumber() : null,
                a.getUser() != null ? a.getUser().getId() : null,
                a.getSubject(),
                a.getRequestedDate(),
                a.getRequestedTimeSlot(),
                a.getStatus(),
                a.getConfirmedAt(),
                a.getCreatedAt()
        );
    }
}