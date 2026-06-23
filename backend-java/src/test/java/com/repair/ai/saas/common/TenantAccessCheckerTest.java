package com.repair.ai.saas.common;

import com.repair.ai.saas.module.tenant.entity.Tenant;
import com.repair.ai.saas.module.tenant.enums.TenantStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TenantAccessCheckerTest {

    private Tenant makeTenant(String status) {
        Tenant t = new Tenant();
        t.setStatus(status);
        t.setPortalEnabled(true);
        return t;
    }

    private Tenant makeTenant(String status, LocalDateTime expiredAt) {
        Tenant t = new Tenant();
        t.setStatus(status);
        t.setExpiredAt(expiredAt);
        t.setPortalEnabled(true);
        return t;
    }

    // ==================== requireLoginAllowed ====================

    @Nested
    @DisplayName("requireLoginAllowed")
    class LoginAllowed {
        @Test
        @DisplayName("TRIAL 允许登录")
        void trial() {
            assertDoesNotThrow(() -> TenantAccessChecker.requireLoginAllowed(makeTenant("TRIAL")));
        }

        @Test
        @DisplayName("ACTIVE 允许登录")
        void active() {
            assertDoesNotThrow(() -> TenantAccessChecker.requireLoginAllowed(makeTenant("ACTIVE")));
        }

        @Test
        @DisplayName("EXPIRED 允许登录（查看历史数据）")
        void expired() {
            assertDoesNotThrow(() -> TenantAccessChecker.requireLoginAllowed(makeTenant("EXPIRED")));
        }

        @Test
        @DisplayName("SUSPENDED 不允许登录")
        void suspended() {
            var ex = assertThrows(BusinessException.class,
                    () -> TenantAccessChecker.requireLoginAllowed(makeTenant("SUSPENDED")));
            assertTrue(ex.getMessage().contains("已被禁用"));
        }

        @Test
        @DisplayName("CLOSED 不允许登录")
        void closed() {
            var ex = assertThrows(BusinessException.class,
                    () -> TenantAccessChecker.requireLoginAllowed(makeTenant("CLOSED")));
            assertTrue(ex.getMessage().contains("已被禁用"));
        }
    }

    // ==================== requireWriteAllowed ====================

    @Nested
    @DisplayName("requireWriteAllowed")
    class WriteAllowed {
        @Test
        @DisplayName("TRIAL 允许写操作")
        void trial() {
            assertDoesNotThrow(() -> TenantAccessChecker.requireWriteAllowed(makeTenant("TRIAL")));
        }

        @Test
        @DisplayName("ACTIVE 允许写操作")
        void active() {
            assertDoesNotThrow(() -> TenantAccessChecker.requireWriteAllowed(makeTenant("ACTIVE")));
        }

        @Test
        @DisplayName("EXPIRED 不允许写操作")
        void expired() {
            var ex = assertThrows(BusinessException.class,
                    () -> TenantAccessChecker.requireWriteAllowed(makeTenant("EXPIRED")));
            assertTrue(ex.getMessage().contains("已到期"));
        }

        @Test
        @DisplayName("SUSPENDED 不允许写操作")
        void suspended() {
            var ex = assertThrows(BusinessException.class,
                    () -> TenantAccessChecker.requireWriteAllowed(makeTenant("SUSPENDED")));
            assertTrue(ex.getMessage().contains("暂不可用"));
        }

        @Test
        @DisplayName("ACTIVE 但 expiredAt 已过 不允许写操作")
        void activeButExpired() {
            var ex = assertThrows(BusinessException.class,
                    () -> TenantAccessChecker.requireWriteAllowed(
                            makeTenant("ACTIVE", LocalDateTime.now().minusDays(1))));
            assertTrue(ex.getMessage().contains("已到期"));
        }
    }

    // ==================== requirePublicRepairAllowed ====================

    @Nested
    @DisplayName("requirePublicRepairAllowed")
    class PublicRepair {
        @Test
        @DisplayName("TRIAL 允许报修")
        void trial() {
            assertDoesNotThrow(() -> TenantAccessChecker.requirePublicRepairAllowed(makeTenant("TRIAL")));
        }

        @Test
        @DisplayName("ACTIVE 允许报修")
        void active() {
            assertDoesNotThrow(() -> TenantAccessChecker.requirePublicRepairAllowed(makeTenant("ACTIVE")));
        }

        @Test
        @DisplayName("EXPIRED 不允许报修")
        void expired() {
            var ex = assertThrows(BusinessException.class,
                    () -> TenantAccessChecker.requirePublicRepairAllowed(makeTenant("EXPIRED")));
            assertTrue(ex.getMessage().contains("已到期"));
        }

        @Test
        @DisplayName("SUSPENDED 不允许报修")
        void suspended() {
            var ex = assertThrows(BusinessException.class,
                    () -> TenantAccessChecker.requirePublicRepairAllowed(makeTenant("SUSPENDED")));
            assertTrue(ex.getMessage().contains("暂不可用"));
        }
    }

    // ==================== requirePublicQueryAllowed ====================

    @Nested
    @DisplayName("requirePublicQueryAllowed")
    class PublicQuery {
        @Test
        @DisplayName("TRIAL 允许查询")
        void trial() {
            assertDoesNotThrow(() -> TenantAccessChecker.requirePublicQueryAllowed(makeTenant("TRIAL")));
        }

        @Test
        @DisplayName("ACTIVE 允许查询")
        void active() {
            assertDoesNotThrow(() -> TenantAccessChecker.requirePublicQueryAllowed(makeTenant("ACTIVE")));
        }

        @Test
        @DisplayName("EXPIRED 允许查询")
        void expired() {
            assertDoesNotThrow(() -> TenantAccessChecker.requirePublicQueryAllowed(makeTenant("EXPIRED")));
        }

        @Test
        @DisplayName("SUSPENDED 不允许查询")
        void suspended() {
            var ex = assertThrows(BusinessException.class,
                    () -> TenantAccessChecker.requirePublicQueryAllowed(makeTenant("SUSPENDED")));
            assertTrue(ex.getMessage().contains("暂不可用"));
        }

        @Test
        @DisplayName("CLOSED 不允许查询")
        void closed() {
            var ex = assertThrows(BusinessException.class,
                    () -> TenantAccessChecker.requirePublicQueryAllowed(makeTenant("CLOSED")));
            assertTrue(ex.getMessage().contains("暂不可用"));
        }
    }

    // ==================== requireAiAllowed ====================

    @Nested
    @DisplayName("requireAiAllowed")
    class AiAllowed {
        @Test
        @DisplayName("TRIAL 允许 AI 咨询")
        void trial() {
            assertDoesNotThrow(() -> TenantAccessChecker.requireAiAllowed(makeTenant("TRIAL")));
        }

        @Test
        @DisplayName("ACTIVE 允许 AI 咨询")
        void active() {
            assertDoesNotThrow(() -> TenantAccessChecker.requireAiAllowed(makeTenant("ACTIVE")));
        }

        @Test
        @DisplayName("EXPIRED 不允许 AI 咨询")
        void expired() {
            var ex = assertThrows(BusinessException.class,
                    () -> TenantAccessChecker.requireAiAllowed(makeTenant("EXPIRED")));
            assertTrue(ex.getMessage().contains("已到期"));
        }

        @Test
        @DisplayName("ACTIVE 但 expiredAt 已过 不允许 AI 咨询")
        void activeButExpired() {
            var ex = assertThrows(BusinessException.class,
                    () -> TenantAccessChecker.requireAiAllowed(
                            makeTenant("ACTIVE", LocalDateTime.now().minusDays(1))));
            assertTrue(ex.getMessage().contains("已到期"));
        }
    }

    // ==================== isLoginAllowed ====================

    @Test
    @DisplayName("isLoginAllowed: TRIAL/ACTIVE/EXPIRED 返回 true")
    void isLoginAllowed_true() {
        assertTrue(TenantAccessChecker.isLoginAllowed(makeTenant("TRIAL")));
        assertTrue(TenantAccessChecker.isLoginAllowed(makeTenant("ACTIVE")));
        assertTrue(TenantAccessChecker.isLoginAllowed(makeTenant("EXPIRED")));
    }

    @Test
    @DisplayName("isLoginAllowed: SUSPENDED 返回 false")
    void isLoginAllowed_false() {
        assertFalse(TenantAccessChecker.isLoginAllowed(makeTenant("SUSPENDED")));
    }

    // ==================== isExpired ====================

    @Test
    @DisplayName("isExpired: expiredAt 在过去返回 true")
    void isExpired_true() {
        assertTrue(TenantAccessChecker.isExpired(
                makeTenant("ACTIVE", LocalDateTime.now().minusHours(1))));
    }

    @Test
    @DisplayName("isExpired: expiredAt 在未来返回 false")
    void isExpired_false() {
        assertFalse(TenantAccessChecker.isExpired(
                makeTenant("ACTIVE", LocalDateTime.now().plusDays(30))));
    }

    @Test
    @DisplayName("isExpired: expiredAt=null 返回 false")
    void isExpired_null() {
        assertFalse(TenantAccessChecker.isExpired(makeTenant("ACTIVE", null)));
    }
}
