package com.lesieurcristal.b2bportal.appointment.controller;

import com.lesieurcristal.b2bportal.appointment.dto.AppointmentResponseDto;
import com.lesieurcristal.b2bportal.appointment.dto.CreateAppointmentRequest;
import com.lesieurcristal.b2bportal.appointment.service.AppointmentService;
import com.lesieurcristal.b2bportal.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Rendez-vous", description = "Prise de rendez-vous client")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLIENT')")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @Operation(summary = "Réserve un nouveau créneau")
    @PostMapping
    public ResponseEntity<AppointmentResponseDto> create(@Valid @RequestBody CreateAppointmentRequest dto) {
        return ResponseEntity.ok(appointmentService.create(dto));
    }

    @Operation(summary = "Liste de mes rendez-vous")
    @GetMapping
    public ResponseEntity<List<AppointmentResponseDto>> list() {
        return ResponseEntity.ok(appointmentService.listForCurrentUser());
    }
}
