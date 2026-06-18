package com.repair.ai.saas.module.user.controller;

import com.repair.ai.saas.common.ApiResponse;
import com.repair.ai.saas.module.operation.enums.OperationType;
import com.repair.ai.saas.module.operation.service.OperationLogService;
import com.repair.ai.saas.module.user.entity.SysUser;
import com.repair.ai.saas.module.user.service.SysUserService;
import com.repair.ai.saas.security.CurrentUser;
import com.repair.ai.saas.security.CurrentUserInfo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService sysUserService;
    private final OperationLogService operationLogService;

    // ==================== 公开接口 ====================

    @PostMapping("/api/public/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginRequest req) {
        var result = sysUserService.login(req.tenantCode, req.username, req.password);
        return ApiResponse.success(Map.of(
                "token", result.token(),
                "tenantCode", result.tenantCode(),
                "userId", result.userId(),
                "username", result.username(),
                "role", result.role(),
                "realName", result.realName()
        ));
    }

    // ==================== 管理后台接口 ====================

    @PostMapping("/api/admin/users")
    public ApiResponse<Map<String, Object>> createUser(@Valid @RequestBody CreateUserRequest req,
                                                        @CurrentUserInfo CurrentUser currentUser,
                                                        HttpServletRequest request) {
        SysUser user = sysUserService.createUser(currentUser.getTenantId(),
                req.username, req.password, req.realName, req.phone, req.email, req.role);
        operationLogService.record(currentUser.getTenantId(), currentUser.getUserId(),
                currentUser.getUsername(), OperationType.CREATE_USER.name(), "USER",
                String.valueOf(user.getId()), "创建员工: " + user.getRealName(), request.getRemoteAddr());
        return ApiResponse.success(Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "realName", user.getRealName(),
                "role", user.getRole()
        ));
    }

    @GetMapping("/api/admin/users")
    public ApiResponse<Map<String, Object>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @CurrentUserInfo CurrentUser currentUser) {
        var result = sysUserService.listUsers(currentUser.getTenantId(), page, size);
        return ApiResponse.success(Map.of(
                "total", result.getTotal(),
                "page", result.getCurrent(),
                "size", result.getSize(),
                "records", result.getRecords()
        ));
    }

    @GetMapping("/api/admin/users/{id}")
    public ApiResponse<SysUser> getUser(@PathVariable Long id,
                                          @CurrentUserInfo CurrentUser currentUser) {
        return ApiResponse.success(sysUserService.getUserById(currentUser.getTenantId(), id));
    }

    @PutMapping("/api/admin/users/{id}")
    public ApiResponse<Void> updateUser(@PathVariable Long id,
                                         @RequestBody UpdateUserRequest req,
                                         @CurrentUserInfo CurrentUser currentUser,
                                         HttpServletRequest request) {
        sysUserService.updateUser(currentUser.getTenantId(), id,
                req.realName, req.phone, req.email, req.role);
        operationLogService.record(currentUser.getTenantId(), currentUser.getUserId(),
                currentUser.getUsername(), OperationType.UPDATE_USER.name(), "USER",
                String.valueOf(id), "编辑员工", request.getRemoteAddr());
        return ApiResponse.success();
    }

    @PutMapping("/api/admin/users/{id}/status")
    public ApiResponse<Void> updateUserStatus(@PathVariable Long id,
                                               @RequestBody Map<String, String> body,
                                               @CurrentUserInfo CurrentUser currentUser,
                                               HttpServletRequest request) {
        String status = body.get("status");
        sysUserService.updateStatus(currentUser.getTenantId(), id, status);
        operationLogService.record(currentUser.getTenantId(), currentUser.getUserId(),
                currentUser.getUsername(), OperationType.UPDATE_USER_STATUS.name(), "USER",
                String.valueOf(id), "更新员工状态为: " + status, request.getRemoteAddr());
        return ApiResponse.success();
    }

    @GetMapping("/api/admin/technicians")
    public ApiResponse<List<Map<String, Object>>> listTechnicians(
            @CurrentUserInfo CurrentUser currentUser) {
        var list = sysUserService.listTechnicians(currentUser.getTenantId());
        return ApiResponse.success(list.stream().map(u -> Map.of(
                "id", (Object) u.getId(),
                "realName", u.getRealName(),
                "phone", u.getPhone()
        )).collect(Collectors.toList()));
    }

    // ==================== 通用接口 ====================

    @GetMapping("/api/common/profile")
    public ApiResponse<Map<String, Object>> profile(@CurrentUserInfo CurrentUser currentUser) {
        SysUser user = sysUserService.getUserById(currentUser.getTenantId(), currentUser.getUserId());
        return ApiResponse.success(Map.of(
                "userId", user.getId(),
                "tenantId", user.getTenantId(),
                "username", user.getUsername(),
                "realName", user.getRealName(),
                "role", user.getRole(),
                "phone", user.getPhone(),
                "email", user.getEmail()
        ));
    }

    @PutMapping("/api/common/password")
    public ApiResponse<Void> updatePassword(@Valid @RequestBody UpdatePasswordRequest req,
                                             @CurrentUserInfo CurrentUser currentUser) {
        sysUserService.updatePassword(currentUser.getTenantId(), currentUser.getUserId(),
                req.oldPassword, req.newPassword);
        return ApiResponse.success();
    }

    // ==================== DTO ====================

    public record LoginRequest(
            @NotBlank String tenantCode,
            @NotBlank String username,
            @NotBlank String password
    ) {}

    public record CreateUserRequest(
            @NotBlank String username,
            @NotBlank String password,
            @NotBlank String realName,
            String phone,
            String email,
            @NotBlank String role
    ) {}

    public record UpdateUserRequest(
            String realName,
            String phone,
            String email,
            String role
    ) {}

    public record UpdatePasswordRequest(
            @NotBlank String oldPassword,
            @NotBlank String newPassword
    ) {}
}
