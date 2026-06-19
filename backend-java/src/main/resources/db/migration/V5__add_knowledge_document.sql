-- ============================================
-- V0.3.0 文档上传与解析
-- knowledge_document 表 + knowledge_item.document_id
-- ============================================

-- 11. 知识文档表
CREATE TABLE IF NOT EXISTS `knowledge_document` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id` BIGINT NOT NULL COMMENT '所属租户',
    `knowledge_base_id` BIGINT NOT NULL COMMENT '所属知识库ID',
    `original_filename` VARCHAR(255) NOT NULL COMMENT '原始文件名',
    `stored_filename` VARCHAR(255) NOT NULL COMMENT '存储文件名（UUID）',
    `content_type` VARCHAR(100) DEFAULT NULL COMMENT 'MIME 类型',
    `file_size` BIGINT NOT NULL COMMENT '文件大小（字节）',
    `storage_path` VARCHAR(500) NOT NULL COMMENT '存储路径（相对路径）',
    `parse_status` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '解析状态: PENDING/SUCCESS/FAILED',
    `item_count` INT NOT NULL DEFAULT 0 COMMENT '生成的知识条目数',
    `error_message` VARCHAR(1000) DEFAULT NULL COMMENT '解析错误信息',
    `created_by` BIGINT DEFAULT NULL COMMENT '创建人ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_kd_tenant_created` (`tenant_id`, `created_at`),
    KEY `idx_kd_tenant_kb` (`tenant_id`, `knowledge_base_id`),
    CONSTRAINT `fk_kd_kb` FOREIGN KEY (`knowledge_base_id`) REFERENCES `knowledge_base` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识文档';

-- 12. knowledge_item 新增 document_id 字段
ALTER TABLE `knowledge_item`
    ADD COLUMN `document_id` BIGINT DEFAULT NULL COMMENT '来源文档ID（NULL 表示手工创建）' AFTER `sort_order`;

ALTER TABLE `knowledge_item`
    ADD KEY `idx_ki_tenant_document` (`tenant_id`, `document_id`);
