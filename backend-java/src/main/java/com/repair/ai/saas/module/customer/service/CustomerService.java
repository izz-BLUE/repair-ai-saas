package com.repair.ai.saas.module.customer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.repair.ai.saas.common.BusinessException;
import com.repair.ai.saas.common.ResultCode;
import com.repair.ai.saas.module.customer.entity.Customer;
import com.repair.ai.saas.module.customer.mapper.CustomerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerMapper customerMapper;

    /** 创建或返回已有客户（同租户同手机号合并，DB 唯一约束兜底并发） */
    @Transactional
    public Customer createOrGetCustomer(Long tenantId, String name, String phone,
                                        String address, String remark) {
        // 按手机号查重
        Customer existing = findByPhone(tenantId, phone);
        if (existing != null) {
            return fillMissingFields(existing, name, address, remark);
        }

        try {
            Customer customer = new Customer();
            customer.setTenantId(tenantId);
            customer.setName(name);
            customer.setPhone(phone);
            customer.setAddress(address);
            customer.setRemark(remark);
            customerMapper.insert(customer);
            return customer;
        } catch (DuplicateKeyException e) {
            // 并发插入冲突：另一个请求先插入了同手机号客户，重新查询返回
            Customer concurrent = findByPhone(tenantId, phone);
            if (concurrent != null) {
                return fillMissingFields(concurrent, name, address, remark);
            }
            throw new BusinessException(ResultCode.CONFLICT, "该手机号已被其他客户使用");
        }
    }

    private Customer findByPhone(Long tenantId, String phone) {
        return customerMapper.selectOne(
                new LambdaQueryWrapper<Customer>()
                        .eq(Customer::getTenantId, tenantId)
                        .eq(Customer::getPhone, phone)
        );
    }

    private Customer fillMissingFields(Customer existing, String name, String address, String remark) {
        boolean needUpdate = false;
        if (address != null && !address.isBlank()
                && (existing.getAddress() == null || existing.getAddress().isBlank())) {
            existing.setAddress(address);
            needUpdate = true;
        }
        if (remark != null && !remark.isBlank()
                && (existing.getRemark() == null || existing.getRemark().isBlank())) {
            existing.setRemark(remark);
            needUpdate = true;
        }
        if (name != null && !name.isBlank()
                && (existing.getName() == null || existing.getName().isBlank())) {
            existing.setName(name);
            needUpdate = true;
        }
        if (needUpdate) {
            customerMapper.updateById(existing);
        }
        return existing;
    }

    public Page<Customer> listCustomers(Long tenantId, int page, int size, String keyword) {
        Page<Customer> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<Customer>()
                .eq(Customer::getTenantId, tenantId);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(Customer::getName, keyword)
                    .or().like(Customer::getPhone, keyword));
        }
        wrapper.orderByDesc(Customer::getCreatedAt);
        return customerMapper.selectPage(pageParam, wrapper);
    }

    public Customer getCustomerById(Long tenantId, Long customerId) {
        Customer customer = customerMapper.selectOne(
                new LambdaQueryWrapper<Customer>()
                        .eq(Customer::getTenantId, tenantId)
                        .eq(Customer::getId, customerId)
        );
        if (customer == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "客户不存在");
        }
        return customer;
    }

    public void updateCustomer(Long tenantId, Long customerId,
                               String name, String phone, String address, String remark) {
        Customer customer = getCustomerById(tenantId, customerId);

        // 如果修改手机号，检查与同租户其他客户冲突
        if (phone != null && !phone.equals(customer.getPhone())) {
            Long count = customerMapper.selectCount(
                    new LambdaQueryWrapper<Customer>()
                            .eq(Customer::getTenantId, tenantId)
                            .eq(Customer::getPhone, phone)
                            .ne(Customer::getId, customerId)
            );
            if (count > 0) {
                throw new BusinessException(ResultCode.CONFLICT, "该手机号已存在其他客户");
            }
            customer.setPhone(phone);
        }
        if (name != null) customer.setName(name);
        if (address != null) customer.setAddress(address);
        if (remark != null) customer.setRemark(remark);
        customerMapper.updateById(customer);
    }
}
