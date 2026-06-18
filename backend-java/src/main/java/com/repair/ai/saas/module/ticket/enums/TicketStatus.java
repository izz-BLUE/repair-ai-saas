package com.repair.ai.saas.module.ticket.enums;

import java.util.EnumSet;
import java.util.Set;

public enum TicketStatus {

    PENDING("待处理"),
    ASSIGNED("已派单"),
    IN_PROGRESS("处理中"),
    COMPLETED("已完成"),
    FOLLOWED_UP("已回访"),
    CLOSED("已关闭"),
    CANCELLED("已取消");

    private final String label;

    TicketStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** 所有合法状态流转 */
    public static Set<TicketStatus> getAllowedTargets(TicketStatus current) {
        return switch (current) {
            case PENDING -> EnumSet.of(ASSIGNED, CANCELLED);
            case ASSIGNED -> EnumSet.of(IN_PROGRESS, CANCELLED);
            case IN_PROGRESS -> EnumSet.of(COMPLETED);
            case COMPLETED -> EnumSet.of(FOLLOWED_UP, CLOSED);
            case FOLLOWED_UP -> EnumSet.of(CLOSED);
            case CLOSED, CANCELLED -> EnumSet.noneOf(TicketStatus.class);
        };
    }

    public boolean canTransitionTo(TicketStatus target) {
        return getAllowedTargets(this).contains(target);
    }

    /**
     * 从字符串解析状态，忽略大小写。
     * 返回 null 表示不合法。
     */
    public static TicketStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return TicketStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
