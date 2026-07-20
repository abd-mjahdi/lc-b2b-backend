package com.lesieurcristal.b2bportal.entity.app.enums;

/**
 * Mirrors the CHECK constraint on app.users.role: CHECK (role IN ('CLIENT','ADMIN')).
 * Values match the enum constant names exactly, so plain EnumType.STRING is safe
 * (no converter needed).
 */
public enum UserRole {
    CLIENT,
    ADMIN
}
