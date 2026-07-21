package com.example.portail_b2b.auth;

public class AuthErrorCodes {
    public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    public static final String ACCOUNT_NOT_ACTIVATED = "ACCOUNT_NOT_ACTIVATED";
    public static final String CUSTOMER_NOT_FOUND = "CUSTOMER_NOT_FOUND";
    public static final String LOGIN_ALREADY_EXISTS = "LOGIN_ALREADY_EXISTS";
    public static final String EMAIL_ALREADY_EXISTS = "EMAIL_ALREADY_EXISTS";
    public static final String INVALID_ACTIVATION_TOKEN = "INVALID_ACTIVATION_TOKEN";
    public static final String ACTIVATION_TOKEN_EXPIRED = "ACTIVATION_TOKEN_EXPIRED";
    public static final String ACCOUNT_ALREADY_ACTIVE = "ACCOUNT_ALREADY_ACTIVE";

    private AuthErrorCodes() {
    }
}
