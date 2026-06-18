package com.repair.ai.saas.module.ticket.enums;

public enum TicketPriority {

    LOW("低"),
    NORMAL("普通"),
    HIGH("高"),
    URGENT("紧急");

    private final String label;

    TicketPriority(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * 从字符串解析优先级，忽略大小写。
     * 返回 null 表示不合法。
     */
    public static TicketPriority fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return TicketPriority.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
