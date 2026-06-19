package com.repair.ai.saas.module.platform.controller;

import com.repair.ai.saas.common.ApiResponse;
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

import java.security.SecureRandom;
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

    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789!@#$%";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

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
        String adminPassword = generateRandomPassword(12);
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
     */
    @PutMapping("/{id}")
    public ApiResponse<Void> updateTenant(@PathVariable Long id,
                                          @RequestBody UpdateTenantRequest req,
                                          @CurrentUserInfo CurrentUser currentUser) {
        RoleChecker.requireSuperAdmin(currentUser);
        tenantService.updateTenantByPlatform(
                id, req.name, req.contactName, req.contactPhone, req.address,
                req.maxKnowledgeBases, req.maxDocuments, req.maxAiDailyCalls,
                req.expiredAt
        );
        return ApiResponse.success();
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
        String newPassword = generateRandomPassword(12);
        sysUserService.resetAdminPassword(id, newPassword);
        return ApiResponse.success(Map.of("newPassword", newPassword));
    }

    // ==================== 工具方法 ====================

    private static String generateRandomPassword(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(PASSWORD_CHARS.charAt(SECURE_RANDOM.nextInt(PASSWORD_CHARS.length())));
        }
        return sb.toString();
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
