package com.repair.ai.saas.security;

import com.repair.ai.saas.common.BusinessException;
import com.repair.ai.saas.common.ResultCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RoleCheckerTest {

    private CurrentUser user(Long id, String role) {
        return new CurrentUser(id, 1L, role.toLowerCase(), role, "TEST01");
    }

    private CurrentUser platformSuperAdmin() {
        return new CurrentUser(1L, 1L, "superadmin", "SUPER_ADMIN", "PLATFORM");
    }

    private CurrentUser nonPlatformSuperAdmin() {
        return new CurrentUser(99L, 99L, "badactor", "SUPER_ADMIN", "TA7P55N");
    }

    // ===== requireAdmin =====

    @Test
    void requireAdmin_admin_passes() {
        assertDoesNotThrow(() -> RoleChecker.requireAdmin(user(1L, "ADMIN")));
    }

    @Test
    void requireAdmin_dispatcher_throwsForbidden() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> RoleChecker.requireAdmin(user(2L, "DISPATCHER")));
        assertEquals(ResultCode.FORBIDDEN, ex.getCode());
    }

    @Test
    void requireAdmin_technician_throwsForbidden() {
        assertThrows(BusinessException.class,
                () -> RoleChecker.requireAdmin(user(3L, "TECHNICIAN")));
    }

    @Test
    void requireAdmin_null_throwsForbidden() {
        assertThrows(BusinessException.class,
                () -> RoleChecker.requireAdmin(null));
    }

    // ===== requireAdminOrDispatcher =====

    @Test
    void requireAdminOrDispatcher_admin_passes() {
        assertDoesNotThrow(() -> RoleChecker.requireAdminOrDispatcher(user(1L, "ADMIN")));
    }

    @Test
    void requireAdminOrDispatcher_dispatcher_passes() {
        assertDoesNotThrow(() -> RoleChecker.requireAdminOrDispatcher(user(2L, "DISPATCHER")));
    }

    @Test
    void requireAdminOrDispatcher_technician_throwsForbidden() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> RoleChecker.requireAdminOrDispatcher(user(3L, "TECHNICIAN")));
        assertEquals(ResultCode.FORBIDDEN, ex.getCode());
    }

    @Test
    void requireAdminOrDispatcher_null_throwsForbidden() {
        assertThrows(BusinessException.class,
                () -> RoleChecker.requireAdminOrDispatcher(null));
    }

    // ===== requireTechnician =====

    @Test
    void requireTechnician_technician_passes() {
        assertDoesNotThrow(() -> RoleChecker.requireTechnician(user(3L, "TECHNICIAN")));
    }

    @Test
    void requireTechnician_admin_throwsForbidden() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> RoleChecker.requireTechnician(user(1L, "ADMIN")));
        assertEquals(ResultCode.FORBIDDEN, ex.getCode());
    }

    @Test
    void requireTechnician_dispatcher_throwsForbidden() {
        assertThrows(BusinessException.class,
                () -> RoleChecker.requireTechnician(user(2L, "DISPATCHER")));
    }

    @Test
    void requireTechnician_null_throwsForbidden() {
        assertThrows(BusinessException.class,
                () -> RoleChecker.requireTechnician(null));
    }

    // ===== requirePlatformSuperAdmin =====

    @Test
    void requirePlatformSuperAdmin_platformSuperAdmin_passes() {
        assertDoesNotThrow(() -> RoleChecker.requirePlatformSuperAdmin(platformSuperAdmin()));
    }

    @Test
    void requirePlatformSuperAdmin_nonPlatformSuperAdmin_throwsForbidden() {
        // 非 PLATFORM 租户的 SUPER_ADMIN（越权场景）→ 拒绝
        BusinessException ex = assertThrows(BusinessException.class,
                () -> RoleChecker.requirePlatformSuperAdmin(nonPlatformSuperAdmin()));
        assertEquals(ResultCode.FORBIDDEN, ex.getCode());
    }

    @Test
    void requirePlatformSuperAdmin_tenantAdmin_throwsForbidden() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> RoleChecker.requirePlatformSuperAdmin(user(1L, "ADMIN")));
        assertEquals(ResultCode.FORBIDDEN, ex.getCode());
    }

    @Test
    void requirePlatformSuperAdmin_technician_throwsForbidden() {
        assertThrows(BusinessException.class,
                () -> RoleChecker.requirePlatformSuperAdmin(user(3L, "TECHNICIAN")));
    }

    @Test
    void requirePlatformSuperAdmin_null_throwsForbidden() {
        assertThrows(BusinessException.class,
                () -> RoleChecker.requirePlatformSuperAdmin(null));
    }

    // ===== requireSuperAdmin（旧方法，向后兼容） =====

    @Test
    void requireSuperAdmin_superAdmin_passes() {
        assertDoesNotThrow(() -> RoleChecker.requireSuperAdmin(platformSuperAdmin()));
    }

    @Test
    void requireSuperAdmin_tenantAdmin_throwsForbidden() {
        assertThrows(BusinessException.class,
                () -> RoleChecker.requireSuperAdmin(user(1L, "ADMIN")));
    }
}
