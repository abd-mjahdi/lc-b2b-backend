package com.example.portail_b2b.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Size(max = 50) String login,
        @NotBlank @Size(min = 8, max = 128) String password
) {
}
