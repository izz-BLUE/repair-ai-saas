package com.repair.ai.saas.security;

public final class UserContext {

    private static final ThreadLocal<CurrentUser> CURRENT_USER = new ThreadLocal<>();

    private UserContext() {}

    public static void set(CurrentUser user) {
        CURRENT_USER.set(user);
    }

    public static CurrentUser get() {
        return CURRENT_USER.get();
    }

    public static void clear() {
        CURRENT_USER.remove();
    }

    public static Long getTenantId() {
        CurrentUser user = get();
        return user != null ? user.getTenantId() : null;
    }

    public static Long getUserId() {
        CurrentUser user = get();
        return user != null ? user.getUserId() : null;
    }
}
