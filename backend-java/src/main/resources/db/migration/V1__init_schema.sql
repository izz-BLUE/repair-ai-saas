-- ============================================
-- repair-ai-saas V0.1 初始化建表脚本
-- 所有业务表包含 tenant_id
-- ============================================

-- 1. 租户/企业表
CREATE TABLE IF NOT EXISTS `tenant` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_code` VARCHAR(20) NOT NULL COMMENT '租户编码（公开标识，如 TABC123）',
    `name` VARCHAR(100) NOT NULL COMMENT '企业名称',
    `contact_name` VARCHAR(50) DEFAULT NULL COMMENT '联系人',
    `contact_phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
    `address` VARCHAR(255) DEFAULT NULL COMMENT '地址',
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/INACTIVE',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0=未删除, 1=已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_code` (`tenant_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='企业/租户';

-- 2. 员工账号表
CREATE TABLE IF NOT EXISTS `sys_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id` BIGINT NOT NULL COMMENT '所属租户',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `role` VARCHAR(20) NOT NULL COMMENT '角色: ADMIN/DISPATCHER/TECHNICIAN',
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/INACTIVE',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_username` (`tenant_id`, `username`),
    KEY `idx_tenant_role_status` (`tenant_id`, `role`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='员工账号';

-- 3. 客户表
CREATE TABLE IF NOT EXISTS `customer` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id` BIGINT NOT NULL COMMENT '所属租户',
    `name` VARCHAR(50) NOT NULL COMMENT '客户姓名',
    `phone` VARCHAR(20) NOT NULL COMMENT '手机号',
    `address` VARCHAR(255) DEFAULT NULL COMMENT '地址',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_phone` (`tenant_id`, `phone`),
    KEY `idx_tenant_name` (`tenant_id`, `name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='客户';

-- 4. 维修工单表
CREATE TABLE IF NOT EXISTS `repair_ticket` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id` BIGINT NOT NULL COMMENT '所属租户',
    `ticket_no` VARCHAR(32) NOT NULL COMMENT '工单编号（租户内唯一）',
    `customer_id` BIGINT NOT NULL COMMENT '客户ID',
    `customer_name` VARCHAR(50) DEFAULT NULL COMMENT '客户姓名（冗余）',
    `customer_phone` VARCHAR(20) DEFAULT NULL COMMENT '客户手机号（冗余）',
    `service_address` VARCHAR(255) DEFAULT NULL COMMENT '服务地址',
    `product_type` VARCHAR(50) DEFAULT NULL COMMENT '产品类型',
    `fault_type` VARCHAR(50) DEFAULT NULL COMMENT '故障类型',
    `fault_description` VARCHAR(1000) DEFAULT NULL COMMENT '故障描述',
    `priority` VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '优先级: LOW/NORMAL/HIGH/URGENT',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '工单状态',
    `technician_id` BIGINT DEFAULT NULL COMMENT '指派师傅ID（sys_user.id）',
    `scheduled_time` DATETIME DEFAULT NULL COMMENT '预约上门时间',
    `start_time` DATETIME DEFAULT NULL COMMENT '上门开始时间',
    `completion_time` DATETIME DEFAULT NULL COMMENT '完成时间',
    `repair_result` VARCHAR(2000) DEFAULT NULL COMMENT '维修结果',
    `cost_note` VARCHAR(500) DEFAULT NULL COMMENT '费用备注',
    `parts_note` VARCHAR(500) DEFAULT NULL COMMENT '配件备注',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_ticket_no` (`tenant_id`, `ticket_no`),
    KEY `idx_tenant_status` (`tenant_id`, `status`),
    KEY `idx_tenant_customer` (`tenant_id`, `customer_id`),
    KEY `idx_tenant_technician` (`tenant_id`, `technician_id`),
    KEY `idx_tenant_created` (`tenant_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='维修工单';

-- 5. 工单状态流转日志表
CREATE TABLE IF NOT EXISTS `ticket_status_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id` BIGINT NOT NULL COMMENT '所属租户',
    `ticket_id` BIGINT NOT NULL COMMENT '工单ID',
    `from_status` VARCHAR(20) DEFAULT NULL COMMENT '变更前状态（新建时为NULL）',
    `to_status` VARCHAR(20) NOT NULL COMMENT '变更后状态',
    `operator_id` BIGINT DEFAULT NULL COMMENT '操作人ID（sys_user.id）',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_ticket_id` (`ticket_id`),
    KEY `idx_tenant_ticket` (`tenant_id`, `ticket_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工单状态流转日志';

-- 6. 操作日志表
CREATE TABLE IF NOT EXISTS `operation_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id` BIGINT NOT NULL COMMENT '所属租户',
    `operator_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
    `operator_name` VARCHAR(50) DEFAULT NULL COMMENT '操作人姓名',
    `operation_type` VARCHAR(50) NOT NULL COMMENT '操作类型',
    `target_type` VARCHAR(50) NOT NULL COMMENT '操作对象类型',
    `target_id` VARCHAR(100) DEFAULT NULL COMMENT '操作对象ID',
    `description` VARCHAR(1000) DEFAULT NULL COMMENT '操作描述',
    `request_ip` VARCHAR(50) DEFAULT NULL COMMENT '请求IP',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_created` (`tenant_id`, `created_at`),
    KEY `idx_tenant_type_target` (`tenant_id`, `target_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='操作日志';
