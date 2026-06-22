package com.repair.ai.saas.module.tenant.controller;

import com.repair.ai.saas.common.ApiResponse;
import com.repair.ai.saas.module.tenant.service.TenantService;
import com.repair.ai.saas.module.user.service.SysUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class TenantController {

    private final SysUserService sysUserService;
    private final TenantService tenantService;

    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> register(@Valid @RequestBody RegisterRequest req) {
        var result = sysUserService.register(
                req.tenantName, req.contactName, req.contactPhone,
                req.username, req.password
        );
        return ApiResponse.success(Map.of(
                "token", result.token(),
                "tenantCode", result.tenantCode(),
                "userId", result.userId(),
                "username", result.username(),
                "role", result.role(),
                "realName", result.realName()
        ));
    }

    public record RegisterRequest(
            @NotBlank String tenantName,
            @NotBlank String contactName,
            @NotBlank String contactPhone,
            @NotBlank String username,
            @NotBlank String password
    ) {}

    /**
     * 公开门户配置（客户门户用，无需登录）
     * 返回门户可见配置；tenant 不存在返回 404；portalEnabled 包含 tenant.status 和 portal_enabled 综合判断
     */
    @GetMapping("/{tenantCode}/portal-settings")
    public ApiResponse<Map<String, Object>> getPortalSettings(@PathVariable String tenantCode) {
        Map<String, Object> settings = tenantService.getPublicPortalSettings(tenantCode);
        return ApiResponse.success(settings);
    }
}
