package com.lesieurcristal.b2bportal.auth.dto;

import com.lesieurcristal.b2bportal.entity.app.enums.UserRole;

public record AuthUserResponse(
        Long id,
        String login,
        String email,
        String firstName,
        String lastName,
        UserRole role,
        String customerNumber,
        String language,
        boolean active,
        boolean deactivated
) {
}
