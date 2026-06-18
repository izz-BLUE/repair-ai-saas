-- ============================================
-- V0.2.0 FAQ 知识库
-- 知识库 + 知识条目
-- ============================================

-- 7. 知识库表
CREATE TABLE IF NOT EXISTS `knowledge_base` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id` BIGINT NOT NULL COMMENT '所属租户',
    `name` VARCHAR(100) NOT NULL COMMENT '知识库名称',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '知识库描述',
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/INACTIVE',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_kb_tenant_status` (`tenant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识库';

-- 8. 知识条目表
CREATE TABLE IF NOT EXISTS `knowledge_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id` BIGINT NOT NULL COMMENT '所属租户',
    `knowledge_base_id` BIGINT NOT NULL COMMENT '所属知识库ID',
    `title` VARCHAR(200) NOT NULL COMMENT '标题',
    `question` VARCHAR(1000) NOT NULL COMMENT '常见问题',
    `answer` VARCHAR(5000) NOT NULL COMMENT '解答',
    `product_type` VARCHAR(50) DEFAULT NULL COMMENT '产品类型（可选）',
    `fault_type` VARCHAR(50) DEFAULT NULL COMMENT '故障类型（可选）',
    `keywords` VARCHAR(500) DEFAULT NULL COMMENT '关键词，逗号分隔',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序序号',
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/INACTIVE',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_ki_tenant_base` (`tenant_id`, `knowledge_base_id`),
    KEY `idx_ki_tenant_product_fault` (`tenant_id`, `product_type`, `fault_type`),
    CONSTRAINT `fk_ki_kb` FOREIGN KEY (`knowledge_base_id`) REFERENCES `knowledge_base` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识条目';
