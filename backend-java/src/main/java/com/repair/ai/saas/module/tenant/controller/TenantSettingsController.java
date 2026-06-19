package com.repair.ai.saas.module.tenant.controller;

import com.repair.ai.saas.common.ApiResponse;
import com.repair.ai.saas.module.tenant.entity.Tenant;
import com.repair.ai.saas.module.tenant.service.TenantService;
import com.repair.ai.saas.security.CurrentUser;
import com.repair.ai.saas.security.CurrentUserInfo;
import com.repair.ai.saas.security.RoleChecker;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 租户门户配置接口（租户 ADMIN 可操作）
 */
@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
public class TenantSettingsController {

    private final TenantService tenantService;

    /**
     * 获取当前租户门户配置
     */
    @GetMapping
    public ApiResponse<Map<String, Object>> getSettings(@CurrentUserInfo CurrentUser currentUser) {
        RoleChecker.requireAdmin(currentUser);
        Tenant tenant = tenantService.getTenantSettings(currentUser.getTenantId());
        return ApiResponse.success(Map.of(
                "tenantCode", tenant.getTenantCode(),
                "name", tenant.getName() != null ? tenant.getName() : "",
                "portalTitle", tenant.getPortalTitle() != null ? tenant.getPortalTitle() : "",
                "portalDescription", tenant.getPortalDescription() != null ? tenant.getPortalDescription() : "",
                "contactPhone", tenant.getContactPhone() != null ? tenant.getContactPhone() : "",
                "logoUrl", tenant.getLogoUrl() != null ? tenant.getLogoUrl() : "",
                "themeColor", tenant.getThemeColor() != null ? tenant.getThemeColor() : "",
                "portalEnabled", Boolean.TRUE.equals(tenant.getPortalEnabled())
        ));
    }

    /**
     * 更新当前租户门户配置
     * 只允许修改：portalTitle, portalDescription, contactPhone, logoUrl, themeColor, portalEnabled
     */
    @PutMapping
    public ApiResponse<Void> updateSettings(@CurrentUserInfo CurrentUser currentUser,
                                            @RequestBody UpdateSettingsRequest req) {
        RoleChecker.requireAdmin(currentUser);
        tenantService.updatePortalSettings(
                currentUser.getTenantId(),
                req.portalTitle,
                req.portalDescription,
                req.contactPhone,
                req.logoUrl,
                req.themeColor,
                req.portalEnabled
        );
        return ApiResponse.success();
    }

    public record UpdateSettingsRequest(
            @Size(max = 100, message = "门户标题不超过100字符") String portalTitle,
            @Size(max = 500, message = "门户描述不超过500字符") String portalDescription,
            String contactPhone,
            String logoUrl,
            String themeColor,
            Boolean portalEnabled
    ) {}
}
