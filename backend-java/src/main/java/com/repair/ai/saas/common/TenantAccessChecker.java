package com.repair.ai.saas.common;

import com.repair.ai.saas.module.tenant.entity.Tenant;
import com.repair.ai.saas.module.tenant.enums.TenantStatus;

/**
 * 租户访问权限统一校验工具。
 * <p>
 * 根据租户状态（{@link TenantStatus}）判断是否允许各类操作。
 * 替代散落在各处的 {@code "ACTIVE".equals(tenant.getStatus())} 硬编码。
 *
 * <p>使用方式：
 * <pre>
 *   Tenant tenant = tenantService.getByTenantCode(tenantCode);
 *   TenantAccessChecker.requirePublicRepairAllowed(tenant);
 * </pre>
 */
public final class TenantAccessChecker {

    private TenantAccessChecker() {}

    // ─────── 带校验的 require 方法 ───────

    /** 要求租户允许后端登录（TRIAL / ACTIVE / EXPIRED）。否则抛出 FORBIDDEN。 */
    public static void requireLoginAllowed(Tenant tenant) {
        TenantStatus status = resolveStatus(tenant);
        if (!status.allowsLogin()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "该企业已被禁用，请联系平台管理员");
        }
    }

    /** 要求租户允许写操作（TRIAL / ACTIVE）。否则抛出 FORBIDDEN。 */
    public static void requireWriteAllowed(Tenant tenant) {
        checkNotExpiredStatus(tenant, resolveStatus(tenant));
        TenantStatus status = resolveStatus(tenant);
        if (!status.allowsWrite()) {
            throw new BusinessException(ResultCode.FORBIDDEN,
                    status == TenantStatus.EXPIRED ? "该企业服务已到期，请联系平台管理员" : "该企业服务暂不可用");
        }
    }

    /** 要求租户允许公开报修（TRIAL / ACTIVE）。否则抛出 FORBIDDEN。 */
    public static void requirePublicRepairAllowed(Tenant tenant) {
        checkNotExpiredStatus(tenant, resolveStatus(tenant));
        TenantStatus status = resolveStatus(tenant);
        if (!status.allowsPublicRepair()) {
            throw new BusinessException(ResultCode.FORBIDDEN,
                    status == TenantStatus.EXPIRED ? "该企业服务已到期" : "该企业服务暂不可用");
        }
    }

    /** 要求租户允许公开查询工单（TRIAL / ACTIVE / EXPIRED）。否则抛出 FORBIDDEN。 */
    public static void requirePublicQueryAllowed(Tenant tenant) {
        TenantStatus status = resolveStatus(tenant);
        if (!status.allowsPublicQuery()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "该企业服务暂不可用");
        }
    }

    /** 要求租户允许 AI 咨询（TRIAL / ACTIVE）。EXPIRED 不允许。否则抛出 FORBIDDEN。 */
    public static void requireAiAllowed(Tenant tenant) {
        checkNotExpiredStatus(tenant, resolveStatus(tenant));
        TenantStatus status = resolveStatus(tenant);
        if (!status.allowsAi()) {
            throw new BusinessException(ResultCode.FORBIDDEN,
                    status == TenantStatus.EXPIRED ? "该企业服务已到期" : "该企业服务暂不可用");
        }
    }

    // ─────── 无异常的判断方法 ───────

    /** 租户是否允许后端登录。 */
    public static boolean isLoginAllowed(Tenant tenant) {
        return resolveStatus(tenant).allowsLogin();
    }

    /** 租户是否允许写操作。 */
    public static boolean isWriteAllowed(Tenant tenant) {
        return resolveStatus(tenant).allowsWrite() && !isExpired(tenant);
    }

    /** 租户是否已到期。 */
    public static boolean isExpired(Tenant tenant) {
        return tenant.getExpiredAt() != null
                && tenant.getExpiredAt().isBefore(java.time.LocalDateTime.now());
    }

    // ─────── 内部方法 ───────

    private static TenantStatus resolveStatus(Tenant tenant) {
        TenantStatus status = TenantStatus.fromString(tenant.getStatus());
        if (status == null) {
            // 数据库有未知状态值，保守拒绝
            throw new BusinessException(ResultCode.FORBIDDEN, "该企业服务暂不可用");
        }
        return status;
    }

    private static void checkNotExpiredStatus(Tenant tenant, TenantStatus status) {
        // EXPIRED 状态本身在 TenantStatus 中 hasWrite/hasAi 已返回 false，
        // 但为了兼容仅配置了 expiredAt 的老租户（status 仍为 ACTIVE 但已过期），
        // 额外检查 expiredAt 字段
        if (isExpired(tenant)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "该企业服务已到期");
        }
    }
}
