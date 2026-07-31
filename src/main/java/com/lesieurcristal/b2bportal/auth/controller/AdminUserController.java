package com.lesieurcristal.b2bportal.auth.controller;

import com.lesieurcristal.b2bportal.auth.dto.AdminRegisterClientRequest;
import com.lesieurcristal.b2bportal.auth.dto.AdminRegisterClientResponse;
import com.lesieurcristal.b2bportal.auth.service.AdminUserRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.lesieurcristal.b2bportal.auth.dto.SendActivationEmailRequest;
import com.lesieurcristal.b2bportal.auth.dto.SendActivationEmailResponse;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserRegistrationService adminUserRegistrationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminRegisterClientResponse registerClient(@Valid @RequestBody AdminRegisterClientRequest request) {
        return adminUserRegistrationService.registerClient(request);
    }

    @PostMapping("/{userId}/send-activation")
    public SendActivationEmailResponse sendActivationEmail(
            @PathVariable Long userId,
            @RequestBody(required = false) SendActivationEmailRequest request
    ) {
        return adminUserRegistrationService.sendActivationEmail(userId, request);
    }
}
