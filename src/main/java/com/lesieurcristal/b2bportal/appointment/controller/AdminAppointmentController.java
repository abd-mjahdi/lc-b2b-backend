package com.lesieurcristal.b2bportal.appointment.controller;

import com.lesieurcristal.b2bportal.appointment.dto.AppointmentResponseDto;
import com.lesieurcristal.b2bportal.appointment.service.AppointmentService;
import com.lesieurcristal.b2bportal.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Admin — Rendez-vous", description = "Liste et arbitrage des demandes de rendez-vous")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@RestController
@RequestMapping("/api/admin/appointments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAppointmentController {

    private final AppointmentService appointmentService;

    @Operation(summary = "Liste des rendez-vous (tous clients, ou filtrés par client)")
    @GetMapping
    public ResponseEntity<List<AppointmentResponseDto>> list(
            @RequestParam(required = false) String customerNumber) {
        return ResponseEntity.ok(appointmentService.listAllForAdmin(customerNumber));
    }

    @Operation(summary = "Confirme une demande pending")
    @PutMapping("/{id}/confirm")
    public ResponseEntity<AppointmentResponseDto> confirm(@PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.confirm(id));
    }

    @Operation(summary = "Annule une demande pending")
    @PutMapping("/{id}/cancel")
    public ResponseEntity<AppointmentResponseDto> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.cancel(id));
    }
}
