package com.repair.ai.saas.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QuotaCheckerTest {

    @Test
    @DisplayName("checkQuota: maxAllowed=null 不限，不抛异常")
    void checkQuota_unlimited() {
        assertDoesNotThrow(() -> QuotaChecker.checkQuota(999, null, "任意资源"));
    }

    @Test
    @DisplayName("checkQuota: currentCount < maxAllowed 不抛异常")
    void checkQuota_belowLimit() {
        assertDoesNotThrow(() -> QuotaChecker.checkQuota(3, 5, "员工账号"));
    }

    @Test
    @DisplayName("checkQuota: currentCount == maxAllowed 抛异常（已达上限）")
    void checkQuota_atLimit() {
        var ex = assertThrows(BusinessException.class,
                () -> QuotaChecker.checkQuota(5, 5, "知识库"));
        assertTrue(ex.getMessage().contains("已达上限"));
        assertTrue(ex.getMessage().contains("知识库"));
        assertTrue(ex.getMessage().contains("5"));
    }

    @Test
    @DisplayName("checkQuota: currentCount > maxAllowed 抛异常")
    void checkQuota_aboveLimit() {
        var ex = assertThrows(BusinessException.class,
                () -> QuotaChecker.checkQuota(10, 5, "文档"));
        assertTrue(ex.getMessage().contains("已达上限"));
        assertTrue(ex.getMessage().contains("文档"));
    }

    @Test
    @DisplayName("checkQuota: 资源名称正确包含在错误信息中")
    void checkQuota_resourceNameInMessage() {
        var ex = assertThrows(BusinessException.class,
                () -> QuotaChecker.checkQuota(100, 100, "本月工单"));
        assertTrue(ex.getMessage().contains("本月工单"));
    }

    @Test
    @DisplayName("checkQuota: maxAllowed=0 表示立即超限")
    void checkQuota_zeroMax() {
        var ex = assertThrows(BusinessException.class,
                () -> QuotaChecker.checkQuota(0, 0, "任意资源"));
        assertTrue(ex.getMessage().contains("已达上限"));
    }
}
