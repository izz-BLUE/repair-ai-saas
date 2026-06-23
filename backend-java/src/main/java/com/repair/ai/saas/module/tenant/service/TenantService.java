package com.repair.ai.saas.module.tenant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.repair.ai.saas.common.BusinessException;
import com.repair.ai.saas.common.ResultCode;
import com.repair.ai.saas.module.tenant.entity.Tenant;
import com.repair.ai.saas.module.tenant.enums.PlanPreset;
import com.repair.ai.saas.module.tenant.enums.TenantStatus;
import com.repair.ai.saas.module.tenant.mapper.TenantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantMapper tenantMapper;

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final Random RANDOM = new Random();

    /** 创建租户，返回 tenantCode。默认 TRIAL 状态，试用 14 天。 */
    public Tenant createTenant(String name, String contactName, String contactPhone) {
        Tenant tenant = new Tenant();
        tenant.setTenantCode(generateTenantCode());
        tenant.setName(name);
        tenant.setContactName(contactName);
        tenant.setContactPhone(contactPhone);
        tenant.setStatus(TenantStatus.TRIAL.name());
        tenant.setPortalEnabled(true);
        // 应用试用版预设
        PlanPreset trial = PlanPreset.TRIAL;
        tenant.setPlanCode(trial.getCode());
        tenant.setPlanName(trial.getDisplayName());
        tenant.setMaxUsers(trial.getMaxUsers());
        tenant.setMaxTechnicians(trial.getMaxTechnicians());
        tenant.setMaxKnowledgeBases(trial.getMaxKnowledgeBases());
        tenant.setMaxDocuments(trial.getMaxDocuments());
        tenant.setMaxAiDailyCalls(trial.getMaxAiDailyCalls());
        tenant.setTicketMonthlyLimit(trial.getTicketMonthlyLimit());
        tenant.setTrialEndAt(LocalDateTime.now().plusDays(14));
        tenantMapper.insert(tenant);
        return tenant;
    }

    /** 根据 tenantCode 查询租户 */
    public Tenant getByTenantCode(String tenantCode) {
        Tenant tenant = tenantMapper.selectOne(
                new LambdaQueryWrapper<Tenant>()
                        .eq(Tenant::getTenantCode, tenantCode)
        );
        if (tenant == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "租户不存在");
        }
        return tenant;
    }

    private String generateTenantCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder("T");
            for (int i = 0; i < 6; i++) {
                sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
            }
            String code = sb.toString();
            Long count = tenantMapper.selectCount(
                    new LambdaQueryWrapper<Tenant>().eq(Tenant::getTenantCode, code)
            );
            if (count == 0) {
                return code;
            }
        }
        throw new BusinessException(ResultCode.INTERNAL_ERROR, "生成租户编码失败");
    }

    // ---------- 租户查询 ----------

    /** 根据 ID 查询租户，不存在返回 null */
    public Tenant getById(Long tenantId) {
        return tenantMapper.selectById(tenantId);
    }

    // ---------- 门户配置管理 ----------

    /** 更新租户门户配置（仅允许修改以下字段） */
    public void updatePortalSettings(Long tenantId, String portalTitle, String portalDescription,
                                     String contactPhone, String logoUrl, String themeColor,
                                     Boolean portalEnabled) {
        Tenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "租户不存在");
        }
        if (portalTitle != null) tenant.setPortalTitle(portalTitle);
        if (portalDescription != null) tenant.setPortalDescription(portalDescription);
        if (contactPhone != null) tenant.setContactPhone(contactPhone);
        if (logoUrl != null) tenant.setLogoUrl(logoUrl);
        if (themeColor != null) tenant.setThemeColor(themeColor);
        if (portalEnabled != null) tenant.setPortalEnabled(portalEnabled);
        tenantMapper.updateById(tenant);
    }

    /** 获取当前租户的门户配置（管理后台用，包含所有字段） */
    public Tenant getTenantSettings(Long tenantId) {
        Tenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "租户不存在");
        }
        return tenant;
    }

    /** 获取公开门户配置（客户门户用，仅返回安全字段） */
    public Map<String, Object> getPublicPortalSettings(String tenantCode) {
        Tenant tenant = tenantMapper.selectOne(
                new LambdaQueryWrapper<Tenant>()
                        .eq(Tenant::getTenantCode, tenantCode)
        );
        if (tenant == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "企业不存在");
        }
        TenantStatus status = TenantStatus.fromString(tenant.getStatus());
        boolean portalOk = (status != null && status.allowsPublicRepair())
                && Boolean.TRUE.equals(tenant.getPortalEnabled())
                && !isExpired(tenant);
        boolean expired = isExpired(tenant);
        return Map.of(
                "name", tenant.getName() != null ? tenant.getName() : "",
                "portalTitle", tenant.getPortalTitle() != null ? tenant.getPortalTitle() : "",
                "portalDescription", tenant.getPortalDescription() != null ? tenant.getPortalDescription() : "",
                "contactPhone", tenant.getContactPhone() != null ? tenant.getContactPhone() : "",
                "logoUrl", tenant.getLogoUrl() != null ? tenant.getLogoUrl() : "",
                "themeColor", tenant.getThemeColor() != null ? tenant.getThemeColor() : "",
                "portalEnabled", portalOk,
                "expired", expired
        );
    }

    // ---------- 平台管理 ----------

    /** 租户列表（平台管理用，排除 PLATFORM 租户） */
    public Page<Tenant> listTenantsForPlatform(int page, int size) {
        Page<Tenant> pageParam = new Page<>(page, size);
        return tenantMapper.selectPage(pageParam,
                new LambdaQueryWrapper<Tenant>()
                        .ne(Tenant::getTenantCode, "PLATFORM")
                        .orderByDesc(Tenant::getCreatedAt)
        );
    }

    /**
     * 检查租户是否可用（TRIAL/ACTIVE + 未过期）。
     * 用于 JWT 认证过滤器实时校验。
     */
    public boolean isTenantAvailable(Long tenantId) {
        Tenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) return false;
        TenantStatus status = TenantStatus.fromString(tenant.getStatus());
        if (status == null || !status.isUsable()) return false;
        if (isExpired(tenant)) return false;
        return true;
    }

    /**
     * 检查租户是否允许登录（含 EXPIRED，允许查看历史数据）。
     * 用于 JWT 认证过滤器实时校验。
     */
    public boolean isLoginAllowed(Long tenantId) {
        Tenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) return false;
        TenantStatus status = TenantStatus.fromString(tenant.getStatus());
        return status != null && status.allowsLogin();
    }

    /**
     * 检测 TRIAL 状态租户是否试用到期，到期则自动转为 EXPIRED。
     * 在登录或请求时调用，不依赖定时任务。
     *
     * @return 是否发生了状态变更
     */
    public boolean autoExpireIfTrialEnded(Tenant tenant) {
        if (tenant == null) return false;
        if (!TenantStatus.TRIAL.name().equals(tenant.getStatus())) return false;
        if (tenant.getTrialEndAt() == null) return false;
        if (tenant.getTrialEndAt().isAfter(LocalDateTime.now())) return false;
        // 试用到期，自动转 EXPIRED
        tenant.setStatus(TenantStatus.EXPIRED.name());
        tenantMapper.updateById(tenant);
        return true;
    }

    /** 更新租户（平台管理用，可修改所有字段含到期时间，支持将 nullable 字段设为 null） */
    public void updateTenantByPlatform(Long tenantId, String name, String contactName,
                                       String contactPhone, String address,
                                       Integer maxKnowledgeBases, Integer maxDocuments,
                                       Integer maxAiDailyCalls,
                                       Integer maxUsers, Integer maxTechnicians,
                                       Integer ticketMonthlyLimit,
                                       String planCode, String planName,
                                       Object expiredAt, Object trialEndAt,
                                       Set<String> providedFields) {
        Tenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "租户不存在");
        }
        if ("PLATFORM".equals(tenant.getTenantCode())) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "不能修改平台租户");
        }
        // 非 null 字段直接更新
        if (name != null) tenant.setName(name);
        if (contactName != null) tenant.setContactName(contactName);
        if (contactPhone != null) tenant.setContactPhone(contactPhone);
        if (address != null) tenant.setAddress(address);
        if (planCode != null) tenant.setPlanCode(planCode);
        if (planName != null) tenant.setPlanName(planName);
        tenantMapper.updateById(tenant);

        // nullable 字段：仅对 JSON 中显式出现的字段执行 SET（含 null 值）
        LambdaUpdateWrapper<Tenant> wrapper = new LambdaUpdateWrapper<Tenant>()
                .eq(Tenant::getId, tenantId);
        if (providedFields.contains("maxKnowledgeBases")) {
            wrapper.set(Tenant::getMaxKnowledgeBases, maxKnowledgeBases);
        }
        if (providedFields.contains("maxDocuments")) {
            wrapper.set(Tenant::getMaxDocuments, maxDocuments);
        }
        if (providedFields.contains("maxAiDailyCalls")) {
            wrapper.set(Tenant::getMaxAiDailyCalls, maxAiDailyCalls);
        }
        if (providedFields.contains("maxUsers")) {
            wrapper.set(Tenant::getMaxUsers, maxUsers);
        }
        if (providedFields.contains("maxTechnicians")) {
            wrapper.set(Tenant::getMaxTechnicians, maxTechnicians);
        }
        if (providedFields.contains("ticketMonthlyLimit")) {
            wrapper.set(Tenant::getTicketMonthlyLimit, ticketMonthlyLimit);
        }
        if (providedFields.contains("expiredAt")) {
            LocalDateTime expired = (expiredAt instanceof LocalDateTime) ? (LocalDateTime) expiredAt : null;
            wrapper.set(Tenant::getExpiredAt, expired);
        }
        if (providedFields.contains("trialEndAt")) {
            LocalDateTime trial = (trialEndAt instanceof LocalDateTime) ? (LocalDateTime) trialEndAt : null;
            wrapper.set(Tenant::getTrialEndAt, trial);
        }
        tenantMapper.update(null, wrapper);
    }

    /** 启用租户（设为 ACTIVE，清除 trialEndAt） */
    public void activateTenant(Long tenantId) {
        Tenant tenant = requireTenant(tenantId);
        requireNotPlatform(tenant);
        TenantStatus from = TenantStatus.fromString(tenant.getStatus());
        TenantStatus.validateTransition(from, TenantStatus.ACTIVE, tenant.getTenantCode());
        tenant.setStatus(TenantStatus.ACTIVE.name());
        tenant.setTrialEndAt(null);
        tenantMapper.updateById(tenant);
    }

    /** 暂停租户 */
    public void suspendTenant(Long tenantId) {
        Tenant tenant = requireTenant(tenantId);
        requireNotPlatform(tenant);
        TenantStatus from = TenantStatus.fromString(tenant.getStatus());
        TenantStatus.validateTransition(from, TenantStatus.SUSPENDED, tenant.getTenantCode());
        tenant.setStatus(TenantStatus.SUSPENDED.name());
        tenantMapper.updateById(tenant);
    }

    /** 关闭租户（终态） */
    public void closeTenant(Long tenantId) {
        Tenant tenant = requireTenant(tenantId);
        requireNotPlatform(tenant);
        TenantStatus from = TenantStatus.fromString(tenant.getStatus());
        TenantStatus.validateTransition(from, TenantStatus.CLOSED, tenant.getTenantCode());
        tenant.setStatus(TenantStatus.CLOSED.name());
        tenantMapper.updateById(tenant);
    }

    /** 恢复租户（从 SUSPENDED/EXPIRED → ACTIVE） */
    public void restoreTenant(Long tenantId) {
        Tenant tenant = requireTenant(tenantId);
        requireNotPlatform(tenant);
        TenantStatus from = TenantStatus.fromString(tenant.getStatus());
        TenantStatus.validateTransition(from, TenantStatus.ACTIVE, tenant.getTenantCode());
        tenant.setStatus(TenantStatus.ACTIVE.name());
        tenant.setTrialEndAt(null);
        tenantMapper.updateById(tenant);
    }

    /** 应用套餐预设：设置 planCode + planName，并覆盖所有额度字段 */
    public void applyPlanPreset(Long tenantId, String planCode) {
        Tenant tenant = requireTenant(tenantId);
        requireNotPlatform(tenant);
        PlanPreset preset = PlanPreset.fromCode(planCode);
        if (preset == null) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR,
                    "无效的套餐: " + planCode + "，可选值: TRIAL/STARTER/PRO/LEGACY");
        }
        tenant.setPlanCode(preset.getCode());
        tenant.setPlanName(preset.getDisplayName());
        tenant.setMaxUsers(preset.getMaxUsers());
        tenant.setMaxTechnicians(preset.getMaxTechnicians());
        tenant.setMaxKnowledgeBases(preset.getMaxKnowledgeBases());
        tenant.setMaxDocuments(preset.getMaxDocuments());
        tenant.setMaxAiDailyCalls(preset.getMaxAiDailyCalls());
        tenant.setTicketMonthlyLimit(preset.getTicketMonthlyLimit());
        // TRIAL 套餐设 14 天试用期，其他套餐清除试用期
        if (preset == PlanPreset.TRIAL) {
            tenant.setTrialEndAt(LocalDateTime.now().plusDays(14));
        } else {
            tenant.setTrialEndAt(null);
        }
        tenantMapper.updateById(tenant);
    }

    // ==================== @Deprecated（保留兼容） ====================

    /** @deprecated 使用 {@link #activateTenant(Long)} */
    @Deprecated
    public void enableTenant(Long tenantId) {
        Tenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "租户不存在");
        }
        tenant.setStatus(TenantStatus.ACTIVE.name());
        tenantMapper.updateById(tenant);
    }

    /** @deprecated 使用 {@link #suspendTenant(Long)} */
    @Deprecated
    public void disableTenant(Long tenantId) {
        Tenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "租户不存在");
        }
        if ("PLATFORM".equals(tenant.getTenantCode())) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "不能禁用平台租户");
        }
        tenant.setStatus(TenantStatus.SUSPENDED.name());
        tenantMapper.updateById(tenant);
    }

    // ==================== 内部方法 ====================

    private Tenant requireTenant(Long tenantId) {
        Tenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "租户不存在");
        }
        return tenant;
    }

    private void requireNotPlatform(Tenant tenant) {
        if ("PLATFORM".equals(tenant.getTenantCode())) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "不能操作平台租户");
        }
    }

    private boolean isExpired(Tenant tenant) {
        return tenant.getExpiredAt() != null
                && tenant.getExpiredAt().isBefore(LocalDateTime.now());
    }
}
