package com.repair.ai.saas.module.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.repair.ai.saas.module.customer.entity.Customer;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {
}
