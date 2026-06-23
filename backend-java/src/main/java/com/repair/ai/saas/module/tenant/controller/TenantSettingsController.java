package com.repair.ai.saas.module.tenant.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.repair.ai.saas.common.ApiResponse;
import com.repair.ai.saas.module.ai.entity.AiUsageDaily;
import com.repair.ai.saas.module.ai.mapper.AiUsageDailyMapper;
import com.repair.ai.saas.module.knowledge.document.entity.KnowledgeDocument;
import com.repair.ai.saas.module.knowledge.document.mapper.KnowledgeDocumentMapper;
import com.repair.ai.saas.module.knowledge.entity.KnowledgeBase;
import com.repair.ai.saas.module.knowledge.mapper.KnowledgeBaseMapper;
import com.repair.ai.saas.module.tenant.entity.Tenant;
import com.repair.ai.saas.module.tenant.enums.TenantStatus;
import com.repair.ai.saas.module.tenant.service.TenantService;
import com.repair.ai.saas.module.ticket.entity.RepairTicket;
import com.repair.ai.saas.module.ticket.mapper.RepairTicketMapper;
import com.repair.ai.saas.module.user.entity.SysUser;
import com.repair.ai.saas.module.user.mapper.SysUserMapper;
import com.repair.ai.saas.security.CurrentUser;
import com.repair.ai.saas.security.CurrentUserInfo;
import com.repair.ai.saas.security.Role;
import com.repair.ai.saas.security.RoleChecker;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 租户门户配置接口（租户 ADMIN 可操作）
 */
@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
public class TenantSettingsController {

    private final TenantService tenantService;
    private final SysUserMapper sysUserMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final KnowledgeDocumentMapper knowledgeDocumentMapper;
    private final AiUsageDailyMapper aiUsageDailyMapper;
    private final RepairTicketMapper repairTicketMapper;

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

    // ==================== 套餐用量 ====================

    /**
     * 获取当前租户的套餐信息和用量统计。
     * 企业管理员查看自己的套餐、到期时间、各项用量。
     */
    @GetMapping("/usage")
    public ApiResponse<Map<String, Object>> getUsage(@CurrentUserInfo CurrentUser currentUser) {
        RoleChecker.requireAdmin(currentUser);
        Long tenantId = currentUser.getTenantId();
        Tenant tenant = tenantService.getTenantSettings(tenantId);

        // 统计当前用量
        long currentUsers = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getTenantId, tenantId)
                        .eq(SysUser::getStatus, "ACTIVE"));
        long currentTechnicians = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getTenantId, tenantId)
                        .eq(SysUser::getRole, Role.TECHNICIAN.name())
                        .eq(SysUser::getStatus, "ACTIVE"));
        long currentKnowledgeBases = knowledgeBaseMapper.selectCount(
                new LambdaQueryWrapper<KnowledgeBase>()
                        .eq(KnowledgeBase::getTenantId, tenantId));
        long currentDocuments = knowledgeDocumentMapper.selectCount(
                new LambdaQueryWrapper<KnowledgeDocument>()
                        .eq(KnowledgeDocument::getTenantId, tenantId)
                        .eq(KnowledgeDocument::getDeleted, 0));

        // 今日 AI 调用量
        LocalDate today = LocalDate.now();
        AiUsageDaily todayUsage = aiUsageDailyMapper.selectOne(
                new LambdaQueryWrapper<AiUsageDaily>()
                        .eq(AiUsageDaily::getTenantId, tenantId)
                        .eq(AiUsageDaily::getUsageDate, today));
        long todayAiCalls = todayUsage != null ? todayUsage.getCallCount() : 0;

        // 本月工单数
        long monthlyTickets = repairTicketMapper.selectCount(
                new LambdaQueryWrapper<RepairTicket>()
                        .eq(RepairTicket::getTenantId, tenantId)
                        .ge(RepairTicket::getCreatedAt, today.withDayOfMonth(1).atStartOfDay()));

        // 试用剩余天数
        Long daysUntilTrialEnd = null;
        if (tenant.getTrialEndAt() != null) {
            daysUntilTrialEnd = ChronoUnit.DAYS.between(LocalDateTime.now(), tenant.getTrialEndAt());
            if (daysUntilTrialEnd < 0) daysUntilTrialEnd = 0L;
        }

        // 到期剩余天数
        Long daysUntilExpiry = null;
        if (tenant.getExpiredAt() != null) {
            daysUntilExpiry = ChronoUnit.DAYS.between(LocalDateTime.now(), tenant.getExpiredAt());
            if (daysUntilExpiry < 0) daysUntilExpiry = 0L;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("planCode", tenant.getPlanCode());
        result.put("planName", tenant.getPlanName());
        result.put("status", tenant.getStatus());
        result.put("expiredAt", tenant.getExpiredAt());
        result.put("trialEndAt", tenant.getTrialEndAt());
        result.put("daysUntilTrialEnd", daysUntilTrialEnd);
        result.put("daysUntilExpiry", daysUntilExpiry);

        Map<String, Object> limits = new LinkedHashMap<>();
        limits.put("maxUsers", tenant.getMaxUsers());
        limits.put("maxTechnicians", tenant.getMaxTechnicians());
        limits.put("maxKnowledgeBases", tenant.getMaxKnowledgeBases());
        limits.put("maxDocuments", tenant.getMaxDocuments());
        limits.put("maxAiDailyCalls", tenant.getMaxAiDailyCalls());
        limits.put("ticketMonthlyLimit", tenant.getTicketMonthlyLimit());
        result.put("limits", limits);

        Map<String, Object> usage = new LinkedHashMap<>();
        usage.put("currentUsers", currentUsers);
        usage.put("currentTechnicians", currentTechnicians);
        usage.put("currentKnowledgeBases", currentKnowledgeBases);
        usage.put("currentDocuments", currentDocuments);
        usage.put("todayAiCalls", todayAiCalls);
        usage.put("monthlyTickets", monthlyTickets);
        result.put("usage", usage);

        return ApiResponse.success(result);
    }

}
