package com.repair.ai.saas.module.tenant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.repair.ai.saas.common.BusinessException;
import com.repair.ai.saas.common.ResultCode;
import com.repair.ai.saas.module.tenant.entity.Tenant;
import com.repair.ai.saas.module.tenant.mapper.TenantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantMapper tenantMapper;

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final Random RANDOM = new Random();

    /** 创建租户，返回 tenantCode */
    public Tenant createTenant(String name, String contactName, String contactPhone) {
        Tenant tenant = new Tenant();
        tenant.setTenantCode(generateTenantCode());
        tenant.setName(name);
        tenant.setContactName(contactName);
        tenant.setContactPhone(contactPhone);
        tenant.setStatus("ACTIVE");
        tenant.setPortalEnabled(true);
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
        boolean portalOk = "ACTIVE".equals(tenant.getStatus())
                && Boolean.TRUE.equals(tenant.getPortalEnabled())
                && !(tenant.getExpiredAt() != null && tenant.getExpiredAt().isBefore(LocalDateTime.now()));
        boolean expired = tenant.getExpiredAt() != null && tenant.getExpiredAt().isBefore(LocalDateTime.now());
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

    /** 检查租户是否可用（ACTIVE + 未过期） */
    public boolean isTenantAvailable(Long tenantId) {
        Tenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null || !"ACTIVE".equals(tenant.getStatus())) {
            return false;
        }
        if (tenant.getExpiredAt() != null && tenant.getExpiredAt().isBefore(LocalDateTime.now())) {
            return false;
        }
        return true;
    }

    /** 更新租户（平台管理用，可修改所有字段含到期时间，支持将 nullable 字段设为 null） */
    public void updateTenantByPlatform(Long tenantId, String name, String contactName,
                                       String contactPhone, String address,
                                       Integer maxKnowledgeBases, Integer maxDocuments,
                                       Integer maxAiDailyCalls, Object expiredAt,
                                       java.util.Set<String> providedFields) {
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
        if (providedFields.contains("expiredAt")) {
            LocalDateTime expired = (expiredAt instanceof LocalDateTime) ? (LocalDateTime) expiredAt : null;
            wrapper.set(Tenant::getExpiredAt, expired);
        }
        tenantMapper.update(null, wrapper);
    }

    /** 启用租户 */
    public void enableTenant(Long tenantId) {
        Tenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "租户不存在");
        }
        tenant.setStatus("ACTIVE");
        tenantMapper.updateById(tenant);
    }

    /** 禁用租户 */
    public void disableTenant(Long tenantId) {
        Tenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "租户不存在");
        }
        if ("PLATFORM".equals(tenant.getTenantCode())) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "不能禁用平台租户");
        }
        tenant.setStatus("INACTIVE");
        tenantMapper.updateById(tenant);
    }
}
