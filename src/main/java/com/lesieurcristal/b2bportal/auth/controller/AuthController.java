package com.lesieurcristal.b2bportal.auth.controller;

import com.lesieurcristal.b2bportal.auth.dto.ActivateAccountRequest;
import com.lesieurcristal.b2bportal.auth.dto.ActivateAccountResponse;
import com.lesieurcristal.b2bportal.auth.dto.LoginRequest;
import com.lesieurcristal.b2bportal.auth.dto.LoginResponse;
import com.lesieurcristal.b2bportal.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/activate")
    @ResponseStatus(HttpStatus.OK)
    public ActivateAccountResponse activate(@Valid @RequestBody ActivateAccountRequest request) {
        return authService.activateAccount(request);
    }
}
