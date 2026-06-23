package com.repair.ai.saas.module.tenant.enums;

import com.repair.ai.saas.common.BusinessException;
import com.repair.ai.saas.common.ResultCode;

/**
 * 租户生命周期状态。
 * <p>
 * 状态流转：
 * <pre>
 *   TRIAL ──→ ACTIVE ──→ EXPIRED
 *     │          │           │
 *     └──→ SUSPENDED ←──────┘
 *              │
 *              └──→ CLOSED (终态)
 * </pre>
 */
public enum TenantStatus {

    /** 试用中 */
    TRIAL,
    /** 正式使用 */
    ACTIVE,
    /** 已到期 */
    EXPIRED,
    /** 被平台暂停 */
    SUSPENDED,
    /** 已关闭（终态） */
    CLOSED;

    // ─────── 能力判断 ───────

    /** 是否允许后端用户登录（ADMIN/DISPATCHER 等） */
    public boolean allowsLogin() {
        return this == TRIAL || this == ACTIVE || this == EXPIRED;
    }

    /** 是否允许写操作（创建/编辑工单、管理员工等） */
    public boolean allowsWrite() {
        return this == TRIAL || this == ACTIVE;
    }

    /** 是否允许客户报修 */
    public boolean allowsPublicRepair() {
        return this == TRIAL || this == ACTIVE;
    }

    /** 是否允许客户查询工单进度 */
    public boolean allowsPublicQuery() {
        return this != SUSPENDED && this != CLOSED;
    }

    /** 是否允许 AI 咨询（EXPIRED 不允许） */
    public boolean allowsAi() {
        return this == TRIAL || this == ACTIVE;
    }

    /** 兼容旧 isTenantAvailable 语义（业务操作可用） */
    public boolean isUsable() {
        return this == TRIAL || this == ACTIVE;
    }

    // ─────── 状态转换校验 ───────

    /**
     * 检查是否可以从当前状态转为目标状态。
     */
    public boolean canTransitionTo(TenantStatus target) {
        return switch (this) {
            case TRIAL    -> target == ACTIVE || target == SUSPENDED || target == EXPIRED || target == CLOSED;
            case ACTIVE   -> target == EXPIRED || target == SUSPENDED || target == CLOSED;
            case EXPIRED  -> target == ACTIVE || target == SUSPENDED || target == CLOSED;
            case SUSPENDED -> target == TRIAL || target == ACTIVE || target == CLOSED;
            case CLOSED   -> false;
        };
    }

    /**
     * 验证状态转换合法性，非法时抛出 BusinessException。
     */
    public static void validateTransition(TenantStatus from, TenantStatus to, String tenantCode) {
        if (from == null) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "当前租户状态未知");
        }
        if (from == CLOSED) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "已关闭的租户不能修改状态");
        }
        if (!from.canTransitionTo(to)) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR,
                    String.format("不允许从 %s 转换为 %s", from.name(), to.name()));
        }
    }

    /**
     * 根据字符串解析，不匹配返回 null。
     */
    public static TenantStatus fromString(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
