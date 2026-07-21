package com.lesieurcristal.b2bportal.auth;

import com.lesieurcristal.b2bportal.entity.app.User;

public final class UserAuthSupport {

    private UserAuthSupport() {
    }

    public static boolean isActive(User user) {
        return Boolean.TRUE.equals(user.getIsActive());
    }

    public static String customerNumber(User user) {
        return user.getCustomer() != null ? user.getCustomer().getCustomerNumber() : null;
    }
}
