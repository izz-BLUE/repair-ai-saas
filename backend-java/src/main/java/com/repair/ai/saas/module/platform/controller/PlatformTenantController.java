package com.repair.ai.saas.module.platform.controller;

import com.repair.ai.saas.common.ApiResponse;
import com.repair.ai.saas.common.PasswordGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.repair.ai.saas.module.tenant.entity.Tenant;
import com.repair.ai.saas.module.tenant.service.TenantService;
import com.repair.ai.saas.module.user.service.SysUserService;
import com.repair.ai.saas.security.CurrentUser;
import com.repair.ai.saas.security.CurrentUserInfo;
import com.repair.ai.saas.security.RoleChecker;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 平台管理接口（仅 SUPER_ADMIN 可访问）
 */
@RestController
@RequestMapping("/api/platform/tenants")
@RequiredArgsConstructor
public class PlatformTenantController {

    private final TenantService tenantService;
    private final SysUserService sysUserService;

    /**
     * 租户列表（分页，排除 PLATFORM 租户）
     */
    @GetMapping
    public ApiResponse<Map<String, Object>> listTenants(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @CurrentUserInfo CurrentUser currentUser) {
        RoleChecker.requirePlatformSuperAdmin(currentUser);
        var result = tenantService.listTenantsForPlatform(page, size);
        return ApiResponse.success(Map.of(
                "total", result.getTotal(),
                "page", result.getCurrent(),
                "size", result.getSize(),
                "records", result.getRecords()
        ));
    }

    /**
     * 创建租户（自动生成 tenantCode + ADMIN 账号，随机密码）
     */
    @PostMapping
    public ApiResponse<Map<String, Object>> createTenant(
            @Valid @RequestBody CreateTenantRequest req,
            @CurrentUserInfo CurrentUser currentUser) {
        RoleChecker.requirePlatformSuperAdmin(currentUser);

        // 1. 创建租户（默认 TRIAL 状态 + 试用预设）
        Tenant tenant = tenantService.createTenant(req.name, req.contactName, req.contactPhone);

        // 2. 创建管理员账号（随机密码）
        String adminUsername = "admin";
        String adminPassword = PasswordGenerator.generate(12);
        var admin = sysUserService.createUser(
                tenant.getId(), adminUsername, adminPassword,
                req.contactName, req.contactPhone, null, "ADMIN"
        );

        return ApiResponse.success(Map.of(
                "tenant", tenant,
                "adminUsername", adminUsername,
                "adminPassword", adminPassword
        ));
    }

    /**
     * 租户详情
     */
    @GetMapping("/{id}")
    public ApiResponse<Tenant> getTenant(@PathVariable Long id,
                                         @CurrentUserInfo CurrentUser currentUser) {
        RoleChecker.requirePlatformSuperAdmin(currentUser);
        Tenant tenant = tenantService.getById(id);
        if (tenant == null) {
            return ApiResponse.error("NOT_FOUND", "租户不存在");
        }
        return ApiResponse.success(tenant);
    }

    /**
     * 编辑租户（平台管理员可手动修改任意字段）
     * 使用 @RequestBody Map 接收原始 JSON，区分"未传字段"和"显式传 null"。
     * 仅对 JSON 中出现的 nullable 字段执行 SET（含 null），未出现的字段保持不变。
     *
     * <p>套餐预设覆盖请使用 PUT /{id}/plan</p>
     */
    @SuppressWarnings("unchecked")
    @PutMapping("/{id}")
    public ApiResponse<Void> updateTenant(@PathVariable Long id,
                                          @RequestBody Map<String, Object> body,
                                          @CurrentUserInfo CurrentUser currentUser) {
        RoleChecker.requirePlatformSuperAdmin(currentUser);

        // 解析时间字段
        LocalDateTime expiredAt = parseDateTime(body, "expiredAt");
        boolean clearExpiredAt = body.containsKey("expiredAt") && body.get("expiredAt") == null;
        LocalDateTime trialEndAt = parseDateTime(body, "trialEndAt");
        boolean clearTrialEndAt = body.containsKey("trialEndAt") && body.get("trialEndAt") == null;

        tenantService.updateTenantByPlatform(
                id,
                (String) body.get("name"),
                (String) body.get("contactName"),
                (String) body.get("contactPhone"),
                (String) body.get("address"),
                body.containsKey("maxKnowledgeBases") ? toInteger(body.get("maxKnowledgeBases")) : null,
                body.containsKey("maxDocuments") ? toInteger(body.get("maxDocuments")) : null,
                body.containsKey("maxAiDailyCalls") ? toInteger(body.get("maxAiDailyCalls")) : null,
                body.containsKey("maxUsers") ? toInteger(body.get("maxUsers")) : null,
                body.containsKey("maxTechnicians") ? toInteger(body.get("maxTechnicians")) : null,
                body.containsKey("ticketMonthlyLimit") ? toInteger(body.get("ticketMonthlyLimit")) : null,
                (String) body.get("planCode"),
                (String) body.get("planName"),
                clearExpiredAt ? null : expiredAt,
                clearTrialEndAt ? null : trialEndAt,
                body.keySet()
        );
        return ApiResponse.success();
    }

    // ==================== 状态操作 ====================

    /** 启用租户（设为 ACTIVE） */
    @PostMapping("/{id}/activate")
    public ApiResponse<Void> activateTenant(@PathVariable Long id,
                                            @CurrentUserInfo CurrentUser currentUser) {
        RoleChecker.requirePlatformSuperAdmin(currentUser);
        tenantService.activateTenant(id);
        return ApiResponse.success();
    }

    /** 暂停租户（设为 SUSPENDED） */
    @PostMapping("/{id}/suspend")
    public ApiResponse<Void> suspendTenant(@PathVariable Long id,
                                           @CurrentUserInfo CurrentUser currentUser) {
        RoleChecker.requirePlatformSuperAdmin(currentUser);
        tenantService.suspendTenant(id);
        return ApiResponse.success();
    }

    /** 关闭租户（终态 CLOSED） */
    @PostMapping("/{id}/close")
    public ApiResponse<Void> closeTenant(@PathVariable Long id,
                                         @CurrentUserInfo CurrentUser currentUser) {
        RoleChecker.requirePlatformSuperAdmin(currentUser);
        tenantService.closeTenant(id);
        return ApiResponse.success();
    }

    /** 恢复租户（SUSPENDED/EXPIRED → ACTIVE） */
    @PostMapping("/{id}/restore")
    public ApiResponse<Void> restoreTenant(@PathVariable Long id,
                                           @CurrentUserInfo CurrentUser currentUser) {
        RoleChecker.requirePlatformSuperAdmin(currentUser);
        tenantService.restoreTenant(id);
        return ApiResponse.success();
    }

    /** 应用套餐预设（覆盖所有额度字段） */
    @PutMapping("/{id}/plan")
    public ApiResponse<Void> applyPlanPreset(@PathVariable Long id,
                                             @RequestBody Map<String, String> body,
                                             @CurrentUserInfo CurrentUser currentUser) {
        RoleChecker.requirePlatformSuperAdmin(currentUser);
        String planCode = body.get("planCode");
        if (planCode == null || planCode.isBlank()) {
            return ApiResponse.error("VALIDATION_ERROR", "planCode 不能为空");
        }
        tenantService.applyPlanPreset(id, planCode.trim().toUpperCase());
        return ApiResponse.success();
    }

    // ==================== @Deprecated 兼容 ====================

    /**
     * @deprecated 使用 {@link #activateTenant(Long)}
     */
    @Deprecated
    @PostMapping("/{id}/enable")
    public ApiResponse<Void> enableTenant(@PathVariable Long id,
                                          @CurrentUserInfo CurrentUser currentUser) {
        RoleChecker.requirePlatformSuperAdmin(currentUser);
        tenantService.enableTenant(id);
        return ApiResponse.success();
    }

    /**
     * @deprecated 使用 {@link #suspendTenant(Long)}
     */
    @Deprecated
    @PostMapping("/{id}/disable")
    public ApiResponse<Void> disableTenant(@PathVariable Long id,
                                           @CurrentUserInfo CurrentUser currentUser) {
        RoleChecker.requirePlatformSuperAdmin(currentUser);
        tenantService.disableTenant(id);
        return ApiResponse.success();
    }

    /** 重置租户管理员密码（随机临时密码） */
    @PostMapping("/{id}/reset-admin-password")
    public ApiResponse<Map<String, Object>> resetAdminPassword(
            @PathVariable Long id,
            @CurrentUserInfo CurrentUser currentUser) {
        RoleChecker.requirePlatformSuperAdmin(currentUser);
        String newPassword = PasswordGenerator.generate(12);
        sysUserService.resetAdminPassword(id, newPassword);
        return ApiResponse.success(Map.of("newPassword", newPassword));
    }

    // ==================== 工具方法 ====================

    private static Integer toInteger(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).intValue();
        return Integer.parseInt(val.toString());
    }

    private static LocalDateTime parseDateTime(Map<String, Object> body, String key) {
        if (body.containsKey(key) && body.get(key) != null) {
            return LocalDateTime.parse((String) body.get(key));
        }
        return null;
    }

    // ==================== DTO ====================

    public record CreateTenantRequest(
            @NotBlank(message = "企业名称不能为空") @Size(max = 100) String name,
            String contactName,
            String contactPhone
    ) {}

    public record UpdateTenantRequest(
            String name,
            String contactName,
            String contactPhone,
            String address,
            Integer maxKnowledgeBases,
            Integer maxDocuments,
            Integer maxAiDailyCalls,
            Integer maxUsers,
            Integer maxTechnicians,
            Integer ticketMonthlyLimit,
            String planCode,
            String planName,
            LocalDateTime expiredAt,
            LocalDateTime trialEndAt
    ) {}
}
