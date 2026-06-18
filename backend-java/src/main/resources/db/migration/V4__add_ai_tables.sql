-- ============================================
-- V0.2.1 AI 问答记录
-- 对话 + 消息
-- ============================================

-- 9. AI 对话表
CREATE TABLE IF NOT EXISTS `ai_conversation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id` BIGINT NOT NULL COMMENT '所属租户',
    `customer_phone` VARCHAR(20) DEFAULT NULL COMMENT '客户手机号',
    `customer_name` VARCHAR(50) DEFAULT NULL COMMENT '客户姓名',
    `source` VARCHAR(20) NOT NULL DEFAULT 'PUBLIC_CHAT' COMMENT '来源: PUBLIC_CHAT/ADMIN_TEST',
    `question` VARCHAR(1000) NOT NULL COMMENT '客户问题',
    `answer` VARCHAR(5000) DEFAULT NULL COMMENT 'AI 回答',
    `matched_item_count` INT NOT NULL DEFAULT 0 COMMENT '匹配 FAQ 条目数',
    `should_create_ticket` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否建议报修',
    `model` VARCHAR(50) DEFAULT NULL COMMENT '使用的模型',
    `trace_id` VARCHAR(64) DEFAULT NULL COMMENT '链路追踪ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_ai_conv_tenant_created` (`tenant_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI 对话';

-- 10. AI 消息表
CREATE TABLE IF NOT EXISTS `ai_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id` BIGINT NOT NULL COMMENT '所属租户',
    `conversation_id` BIGINT NOT NULL COMMENT '所属对话ID',
    `role` VARCHAR(20) NOT NULL COMMENT '角色: USER/ASSISTANT/SYSTEM',
    `content` VARCHAR(5000) NOT NULL COMMENT '消息内容',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_ai_msg_tenant_conv` (`tenant_id`, `conversation_id`),
    CONSTRAINT `fk_ai_msg_conv` FOREIGN KEY (`conversation_id`) REFERENCES `ai_conversation` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI 消息';
