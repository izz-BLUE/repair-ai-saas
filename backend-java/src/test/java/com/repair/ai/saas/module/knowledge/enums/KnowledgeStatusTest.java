package com.repair.ai.saas.module.knowledge.enums;

import com.repair.ai.saas.common.BusinessException;
import com.repair.ai.saas.common.ResultCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KnowledgeStatusTest {

    // ===== fromString =====

    @Test
    void fromString_active() {
        assertEquals(KnowledgeStatus.ACTIVE, KnowledgeStatus.fromString("ACTIVE"));
    }

    @Test
    void fromString_inactive() {
        assertEquals(KnowledgeStatus.INACTIVE, KnowledgeStatus.fromString("INACTIVE"));
    }

    @Test
    void fromString_lowerCase() {
        assertEquals(KnowledgeStatus.ACTIVE, KnowledgeStatus.fromString("active"));
    }

    @Test
    void fromString_withSpaces() {
        assertEquals(KnowledgeStatus.INACTIVE, KnowledgeStatus.fromString("  INACTIVE  "));
    }

    @Test
    void fromString_null_returnsNull() {
        assertNull(KnowledgeStatus.fromString(null));
    }

    @Test
    void fromString_blank_returnsNull() {
        assertNull(KnowledgeStatus.fromString("  "));
    }

    @Test
    void fromString_unknown_returnsNull() {
        assertNull(KnowledgeStatus.fromString("UNKNOWN"));
    }

    // ===== parse: 合法值 =====

    @Test
    void parse_active() {
        assertEquals(KnowledgeStatus.ACTIVE, KnowledgeStatus.parse("ACTIVE"));
    }

    @Test
    void parse_inactive() {
        assertEquals(KnowledgeStatus.INACTIVE, KnowledgeStatus.parse("INACTIVE"));
    }

    // ===== parse: 非法值抛 BusinessException =====

    @Test
    void parse_invalid_throwsBusinessException() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> KnowledgeStatus.parse("INVALID"));
        assertEquals(ResultCode.VALIDATION_ERROR, ex.getCode());
        assertTrue(ex.getMessage().contains("INVALID"));
    }

    @Test
    void parse_null_throwsBusinessException() {
        assertThrows(BusinessException.class, () -> KnowledgeStatus.parse(null));
    }

    // ===== label =====

    @Test
    void label_active() {
        assertEquals("启用", KnowledgeStatus.ACTIVE.getLabel());
    }

    @Test
    void label_inactive() {
        assertEquals("停用", KnowledgeStatus.INACTIVE.getLabel());
    }
}
