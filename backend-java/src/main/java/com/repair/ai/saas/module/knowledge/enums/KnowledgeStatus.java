package com.repair.ai.saas.module.knowledge.enums;

import com.repair.ai.saas.common.BusinessException;
import com.repair.ai.saas.common.ResultCode;

/**
 * 知识库/条目状态枚举，仅允许 ACTIVE / INACTIVE。
 */
public enum KnowledgeStatus {

    ACTIVE("启用"),
    INACTIVE("停用");

    private final String label;

    KnowledgeStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static KnowledgeStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return KnowledgeStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 校验并返回合法状态，不合法则抛出 VALIDATION_ERROR。
     */
    public static KnowledgeStatus parse(String value) {
        KnowledgeStatus status = fromString(value);
        if (status == null) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR,
                    "无效的状态: " + value + "，可选值: ACTIVE/INACTIVE");
        }
        return status;
    }
}
