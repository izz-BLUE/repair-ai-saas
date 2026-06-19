package com.repair.ai.saas.module.knowledge.document.enums;

/**
 * 文档解析状态。
 */
public enum DocumentParseStatus {

    PENDING,
    SUCCESS,
    FAILED;

    public static DocumentParseStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            return PENDING;
        }
        try {
            return DocumentParseStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return PENDING;
        }
    }
}
