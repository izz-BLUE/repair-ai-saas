-- V0.1.2: 客户手机号租户内唯一约束
-- 同一租户下，未删除的客户手机号不允许重复

ALTER TABLE `customer`
    ADD UNIQUE KEY `uk_customer_tenant_phone_deleted` (`tenant_id`, `phone`, `deleted`);
