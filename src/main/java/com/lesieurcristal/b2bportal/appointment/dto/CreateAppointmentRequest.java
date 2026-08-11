package com.lesieurcristal.b2bportal.appointment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateAppointmentRequest(
        @NotNull LocalDate requestedDate,
        @NotBlank @Size(max = 50) String requestedTimeSlot,
        @Size(max = 200) String subject
) {
}