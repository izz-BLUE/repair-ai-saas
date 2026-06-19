-- ============================================
-- repair-ai-saas V0.3.3 商业化与租户配置
-- tenant 表新增门户配置 + 商业化字段
-- 创建 PLATFORM 租户 + SUPER_ADMIN 用户
-- ============================================

-- 1. tenant 表新增字段
ALTER TABLE `tenant`
    ADD COLUMN `portal_title` VARCHAR(100) DEFAULT NULL COMMENT '门户标题' AFTER `address`,
    ADD COLUMN `portal_description` VARCHAR(500) DEFAULT NULL COMMENT '门户描述' AFTER `portal_title`,
    ADD COLUMN `logo_url` VARCHAR(500) DEFAULT NULL COMMENT 'Logo URL' AFTER `portal_description`,
    ADD COLUMN `theme_color` VARCHAR(20) DEFAULT NULL COMMENT '门户主题色（hex）' AFTER `logo_url`,
    ADD COLUMN `portal_enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用门户' AFTER `theme_color`,
    ADD COLUMN `max_knowledge_bases` INT DEFAULT NULL COMMENT '知识库上限（NULL=不限）' AFTER `portal_enabled`,
    ADD COLUMN `max_documents` INT DEFAULT NULL COMMENT '文档上限（NULL=不限）' AFTER `max_knowledge_bases`,
    ADD COLUMN `max_ai_daily_calls` INT DEFAULT NULL COMMENT 'AI日调用上限（NULL=不限）' AFTER `max_documents`,
    ADD COLUMN `expired_at` DATETIME DEFAULT NULL COMMENT '到期时间（NULL=永不过期）' AFTER `max_ai_daily_calls`;

-- 2. 创建 PLATFORM 租户（仅供 SUPER_ADMIN 登录，不参与业务）
INSERT INTO `tenant` (`tenant_code`, `name`, `status`)
VALUES ('PLATFORM', 'Repair AI 平台', 'ACTIVE');

-- 3. 创建平台管理员（密码: Admin@2024，生产部署后务必修改）
INSERT INTO `sys_user` (`tenant_id`, `username`, `password`, `real_name`, `role`, `status`)
VALUES (
    (SELECT `id` FROM `tenant` WHERE `tenant_code` = 'PLATFORM'),
    'superadmin',
    '$2a$10$/jb4QGK.6FIitbOXrnSKS.RW27BPIoUekVy58n8lfhs7oIJNUs7KK',
    '平台管理员',
    'SUPER_ADMIN',
    'ACTIVE'
);
