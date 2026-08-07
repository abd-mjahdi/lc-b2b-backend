package com.lesieurcristal.b2bportal.auth;

import com.lesieurcristal.b2bportal.auth.dto.AuthUserResponse;
import com.lesieurcristal.b2bportal.entity.app.User;

public final class AuthMapper {

    private AuthMapper() {
    }

    public static AuthUserResponse toUserResponse(User user) {
        return new AuthUserResponse(
                user.getId(),
                user.getLogin(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole(),
                UserAuthSupport.customerNumber(user),
                user.getLanguage(),
                UserAuthSupport.isActive(user),
                Boolean.TRUE.equals(user.getIsDeactivated())
        );
    }
}
