package com.example.portail_b2b.auth;

import com.example.portail_b2b.auth.dto.AuthUserResponse;
import com.example.portail_b2b.entity.User;

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
                user.getCustomerNumber(),
                user.getLanguage()
        );
    }
}
