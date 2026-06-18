package com.repair.ai.saas.module.tenant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.repair.ai.saas.common.BusinessException;
import com.repair.ai.saas.common.ResultCode;
import com.repair.ai.saas.module.tenant.entity.Tenant;
import com.repair.ai.saas.module.tenant.mapper.TenantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
}
