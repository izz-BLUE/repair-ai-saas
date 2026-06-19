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
        RoleChecker.requireSuperAdmin(currentUser);
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
            @RequestBody CreateTenantRequest req,
            @CurrentUserInfo CurrentUser currentUser) {
        RoleChecker.requireSuperAdmin(currentUser);

        // 1. 创建租户
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
        RoleChecker.requireSuperAdmin(currentUser);
        Tenant tenant = tenantService.getById(id);
        if (tenant == null) {
            return ApiResponse.error("NOT_FOUND", "租户不存在");
        }
        return ApiResponse.success(tenant);
    }

    /**
     * 编辑租户（平台管理员可修改基础信息、限额和到期时间）
     * 使用 @RequestBody Map 接收原始 JSON，区分"未传字段"和"显式传 null"。
     * 仅对 JSON 中出现的 nullable 字段执行 SET（含 null），未出现的字段保持不变。
     */
    @SuppressWarnings("unchecked")
    @PutMapping("/{id}")
    public ApiResponse<Void> updateTenant(@PathVariable Long id,
                                          @RequestBody java.util.Map<String, Object> body,
                                          @CurrentUserInfo CurrentUser currentUser) {
        RoleChecker.requireSuperAdmin(currentUser);

        // 解析 expiredAt：Jackson 将 ISO 字符串反序列化为 String，需手动转 LocalDateTime
        LocalDateTime expiredAt = null;
        if (body.containsKey("expiredAt") && body.get("expiredAt") != null) {
            expiredAt = LocalDateTime.parse((String) body.get("expiredAt"));
        }
        boolean clearExpiredAt = body.containsKey("expiredAt") && body.get("expiredAt") == null;

        tenantService.updateTenantByPlatform(
                id,
                (String) body.get("name"),
                (String) body.get("contactName"),
                (String) body.get("contactPhone"),
                (String) body.get("address"),
                body.containsKey("maxKnowledgeBases") ? toInteger(body.get("maxKnowledgeBases")) : null,
                body.containsKey("maxDocuments") ? toInteger(body.get("maxDocuments")) : null,
                body.containsKey("maxAiDailyCalls") ? toInteger(body.get("maxAiDailyCalls")) : null,
                clearExpiredAt ? null : expiredAt,
                body.keySet()
        );
        return ApiResponse.success();
    }

    private static Integer toInteger(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).intValue();
        return Integer.parseInt(val.toString());
    }

    /**
     * 启用租户
     */
    @PostMapping("/{id}/enable")
    public ApiResponse<Void> enableTenant(@PathVariable Long id,
                                          @CurrentUserInfo CurrentUser currentUser) {
        RoleChecker.requireSuperAdmin(currentUser);
        tenantService.enableTenant(id);
        return ApiResponse.success();
    }

    /**
     * 禁用租户
     */
    @PostMapping("/{id}/disable")
    public ApiResponse<Void> disableTenant(@PathVariable Long id,
                                           @CurrentUserInfo CurrentUser currentUser) {
        RoleChecker.requireSuperAdmin(currentUser);
        tenantService.disableTenant(id);
        return ApiResponse.success();
    }

    /**
     * 重置租户管理员密码（随机临时密码）
     */
    @PostMapping("/{id}/reset-admin-password")
    public ApiResponse<Map<String, Object>> resetAdminPassword(
            @PathVariable Long id,
            @CurrentUserInfo CurrentUser currentUser) {
        RoleChecker.requireSuperAdmin(currentUser);
        String newPassword = PasswordGenerator.generate(12);
        sysUserService.resetAdminPassword(id, newPassword);
        return ApiResponse.success(Map.of("newPassword", newPassword));
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
            LocalDateTime expiredAt
    ) {}
}
