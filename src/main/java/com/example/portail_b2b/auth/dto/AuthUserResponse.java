package com.example.portail_b2b.auth.dto;

import com.example.portail_b2b.entity.Role;

public record AuthUserResponse(
        Long id,
        String login,
        String email,
        String firstName,
        String lastName,
        Role role,
        String customerNumber,
        String language
) {
}
