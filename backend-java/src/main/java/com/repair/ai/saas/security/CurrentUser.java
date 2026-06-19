package com.repair.ai.saas.security;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CurrentUser {

    private Long userId;
    private Long tenantId;
    private String username;
    private String role;

    public boolean isAdmin() {
        return Role.ADMIN.name().equals(role);
    }

    public boolean isDispatcher() {
        return Role.DISPATCHER.name().equals(role);
    }

    public boolean isTechnician() {
        return Role.TECHNICIAN.name().equals(role);
    }

    public boolean isAdminOrDispatcher() {
        return isAdmin() || isDispatcher();
    }

    public boolean isSuperAdmin() {
        return Role.SUPER_ADMIN.name().equals(role);
    }

    /**
     * 平台管理员：SUPER_ADMIN 可访问平台接口
     */
    public boolean isPlatformAdmin() {
        return isSuperAdmin();
    }
}
