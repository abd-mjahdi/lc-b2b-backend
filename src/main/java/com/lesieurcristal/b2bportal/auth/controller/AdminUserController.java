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
}
