package com.repair.ai.saas.module.tenant.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class TenantStatusTest {

    // ==================== allowsLogin ====================

    @Test
    @DisplayName("allowsLogin: TRIAL/ACTIVE/EXPIRED 允许登录")
    void allowsLogin_allowed() {
        assertTrue(TenantStatus.TRIAL.allowsLogin());
        assertTrue(TenantStatus.ACTIVE.allowsLogin());
        assertTrue(TenantStatus.EXPIRED.allowsLogin());
    }

    @Test
    @DisplayName("allowsLogin: SUSPENDED/CLOSED 不允许登录")
    void allowsLogin_denied() {
        assertFalse(TenantStatus.SUSPENDED.allowsLogin());
        assertFalse(TenantStatus.CLOSED.allowsLogin());
    }

    // ==================== allowsWrite ====================

    @Test
    @DisplayName("allowsWrite: TRIAL/ACTIVE 允许写操作")
    void allowsWrite_allowed() {
        assertTrue(TenantStatus.TRIAL.allowsWrite());
        assertTrue(TenantStatus.ACTIVE.allowsWrite());
    }

    @Test
    @DisplayName("allowsWrite: EXPIRED/SUSPENDED/CLOSED 不允许写操作")
    void allowsWrite_denied() {
        assertFalse(TenantStatus.EXPIRED.allowsWrite());
        assertFalse(TenantStatus.SUSPENDED.allowsWrite());
        assertFalse(TenantStatus.CLOSED.allowsWrite());
    }

    // ==================== allowsPublicRepair ====================

    @Test
    @DisplayName("allowsPublicRepair: TRIAL/ACTIVE 允许客户报修")
    void allowsPublicRepair_allowed() {
        assertTrue(TenantStatus.TRIAL.allowsPublicRepair());
        assertTrue(TenantStatus.ACTIVE.allowsPublicRepair());
    }

    @Test
    @DisplayName("allowsPublicRepair: EXPIRED/SUSPENDED/CLOSED 不允许客户报修")
    void allowsPublicRepair_denied() {
        assertFalse(TenantStatus.EXPIRED.allowsPublicRepair());
        assertFalse(TenantStatus.SUSPENDED.allowsPublicRepair());
        assertFalse(TenantStatus.CLOSED.allowsPublicRepair());
    }

    // ==================== allowsPublicQuery ====================

    @Test
    @DisplayName("allowsPublicQuery: TRIAL/ACTIVE/EXPIRED 允许查询进度")
    void allowsPublicQuery_allowed() {
        assertTrue(TenantStatus.TRIAL.allowsPublicQuery());
        assertTrue(TenantStatus.ACTIVE.allowsPublicQuery());
        assertTrue(TenantStatus.EXPIRED.allowsPublicQuery());
    }

    @Test
    @DisplayName("allowsPublicQuery: SUSPENDED/CLOSED 不允许查询")
    void allowsPublicQuery_denied() {
        assertFalse(TenantStatus.SUSPENDED.allowsPublicQuery());
        assertFalse(TenantStatus.CLOSED.allowsPublicQuery());
    }

    // ==================== allowsAi ====================

    @Test
    @DisplayName("allowsAi: TRIAL/ACTIVE 允许 AI 咨询")
    void allowsAi_allowed() {
        assertTrue(TenantStatus.TRIAL.allowsAi());
        assertTrue(TenantStatus.ACTIVE.allowsAi());
    }

    @Test
    @DisplayName("allowsAi: EXPIRED/SUSPENDED/CLOSED 不允许 AI 咨询")
    void allowsAi_denied() {
        assertFalse(TenantStatus.EXPIRED.allowsAi());
        assertFalse(TenantStatus.SUSPENDED.allowsAi());
        assertFalse(TenantStatus.CLOSED.allowsAi());
    }

    // ==================== isUsable ====================

    @Test
    @DisplayName("isUsable: TRIAL/ACTIVE 返回 true")
    void isUsable_true() {
        assertTrue(TenantStatus.TRIAL.isUsable());
        assertTrue(TenantStatus.ACTIVE.isUsable());
    }

    @Test
    @DisplayName("isUsable: EXPIRED/SUSPENDED/CLOSED 返回 false")
    void isUsable_false() {
        assertFalse(TenantStatus.EXPIRED.isUsable());
        assertFalse(TenantStatus.SUSPENDED.isUsable());
        assertFalse(TenantStatus.CLOSED.isUsable());
    }

    // ==================== 状态转换 ====================

    @ParameterizedTest
    @CsvSource(delimiter = '>', value = {
            "TRIAL > ACTIVE",
            "TRIAL > SUSPENDED",
            "TRIAL > EXPIRED",
            "TRIAL > CLOSED",
            "ACTIVE > EXPIRED",
            "ACTIVE > SUSPENDED",
            "ACTIVE > CLOSED",
            "EXPIRED > ACTIVE",
            "EXPIRED > SUSPENDED",
            "EXPIRED > CLOSED",
            "SUSPENDED > TRIAL",
            "SUSPENDED > ACTIVE",
            "SUSPENDED > CLOSED",
    })
    @DisplayName("合法状态转换")
    void canTransitionTo_valid(TenantStatus from, TenantStatus to) {
        assertTrue(from.canTransitionTo(to),
                from + " → " + to + " 应该是合法转换");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '>', value = {
            "CLOSED > TRIAL",
            "CLOSED > ACTIVE",
            "CLOSED > EXPIRED",
            "CLOSED > SUSPENDED",
            "TRIAL > TRIAL",
            "ACTIVE > ACTIVE",
            "EXPIRED > EXPIRED",
    })
    @DisplayName("非法状态转换")
    void canTransitionTo_invalid(TenantStatus from, TenantStatus to) {
        assertFalse(from.canTransitionTo(to),
                from + " → " + to + " 应该是非法转换");
    }

    // ==================== validateTransition ====================

    @Test
    @DisplayName("validateTransition: 合法转换不抛异常")
    void validateTransition_valid() {
        assertDoesNotThrow(() ->
                TenantStatus.validateTransition(TenantStatus.TRIAL, TenantStatus.ACTIVE, "TEST001"));
    }

    @Test
    @DisplayName("validateTransition: CLOSED 为终态不能转换")
    void validateTransition_closed() {
        var ex = assertThrows(com.repair.ai.saas.common.BusinessException.class, () ->
                TenantStatus.validateTransition(TenantStatus.CLOSED, TenantStatus.ACTIVE, "TEST001"));
        assertTrue(ex.getMessage().contains("已关闭"));
    }

    @Test
    @DisplayName("validateTransition: 非法转换抛异常")
    void validateTransition_invalid() {
        var ex = assertThrows(com.repair.ai.saas.common.BusinessException.class, () ->
                TenantStatus.validateTransition(TenantStatus.ACTIVE, TenantStatus.TRIAL, "TEST001"));
        assertTrue(ex.getMessage().contains("不允许"));
    }

    // ==================== fromString ====================

    @ParameterizedTest
    @CsvSource(value = {
            "TRIAL, TRIAL",
            "trial, TRIAL",
            "ACTIVE, ACTIVE",
            "EXPIRED, EXPIRED",
            "SUSPENDED, SUSPENDED",
            "CLOSED, CLOSED",
    })
    @DisplayName("fromString: 合法字符串解析正确")
    void fromString_valid(String input, TenantStatus expected) {
        assertEquals(expected, TenantStatus.fromString(input));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"UNKNOWN", "INACTIVE", "abc", "  "})
    @DisplayName("fromString: 非法字符串返回 null")
    void fromString_invalid(String input) {
        assertNull(TenantStatus.fromString(input));
    }
}
