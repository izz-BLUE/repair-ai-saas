package com.repair.ai.saas.module.tenant.controller;

import com.repair.ai.saas.common.ApiResponse;
import com.repair.ai.saas.module.user.service.SysUserService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class TenantController {

    private final SysUserService sysUserService;

    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> register(@RequestBody RegisterRequest req) {
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
}
