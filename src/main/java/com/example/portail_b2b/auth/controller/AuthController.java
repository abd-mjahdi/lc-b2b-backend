package com.example.portail_b2b.auth.controller;

import com.example.portail_b2b.auth.dto.ActivateAccountRequest;
import com.example.portail_b2b.auth.dto.ActivateAccountResponse;
import com.example.portail_b2b.auth.dto.LoginRequest;
import com.example.portail_b2b.auth.dto.LoginResponse;
import com.example.portail_b2b.auth.service.AuthService;
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
