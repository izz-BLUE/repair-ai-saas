package com.repair.ai.saas.security;

public enum Role {
    ADMIN,
    DISPATCHER,
    TECHNICIAN;

    public static Role fromString(String role) {
        try {
            return Role.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
