package com.repair.ai.saas.security;

import com.repair.ai.saas.common.BusinessException;
import com.repair.ai.saas.common.ResultCode;

/**
 * 角色校验工具类
 *
 * 在 Controller 方法入口调用，权限不足时抛出 BusinessException(FORBIDDEN)，
 * 由 GlobalExceptionHandler 统一返回 HTTP 403。
 */
public final class RoleChecker {

    private RoleChecker() {}

    /**
     * 仅 ADMIN 可操作
     */
    public static void requireAdmin(CurrentUser user) {
        if (user == null || !user.isAdmin()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "权限不足，仅管理员可操作");
        }
    }

    /**
     * ADMIN 或 DISPATCHER 可操作
     */
    public static void requireAdminOrDispatcher(CurrentUser user) {
        if (user == null || !user.isAdminOrDispatcher()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "权限不足，仅管理员或客服可操作");
        }
    }

    /**
     * 仅 TECHNICIAN 可操作
     */
    public static void requireTechnician(CurrentUser user) {
        if (user == null || !user.isTechnician()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "权限不足，仅师傅可操作");
        }
    }
}
