package com.repair.ai.saas.module.tenant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.repair.ai.saas.module.tenant.entity.Tenant;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TenantMapper extends BaseMapper<Tenant> {
}
