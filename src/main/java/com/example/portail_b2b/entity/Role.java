package com.example.portail_b2b.entity;

public enum Role {
    CLIENT,
    ADMIN;

    public String asAuthority() {
        return "ROLE_" + name();
    }
}
