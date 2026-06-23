-- ============================================
-- repair-ai-saas V0.7.0 租户生命周期与套餐额度
-- tenant 表新增套餐 + 额度字段
-- INACTIVE → SUSPENDED 语义修正
-- 现有租户使用 LEGACY 预设（高额度，不误伤）
-- ============================================

-- 1. 新字段
ALTER TABLE `tenant`
    ADD COLUMN `plan_code` VARCHAR(50) DEFAULT 'TRIAL' COMMENT '套餐标识码' AFTER `max_ai_daily_calls`,
    ADD COLUMN `plan_name` VARCHAR(100) DEFAULT '试用版' COMMENT '套餐名称' AFTER `plan_code`,
    ADD COLUMN `max_users` INT DEFAULT 5 COMMENT '员工账号上限（NULL=不限）' AFTER `plan_name`,
    ADD COLUMN `max_technicians` INT DEFAULT 3 COMMENT '师傅账号上限（NULL=不限）' AFTER `max_users`,
    ADD COLUMN `ticket_monthly_limit` INT DEFAULT 50 COMMENT '月工单上限（NULL=不限）' AFTER `max_technicians`,
    ADD COLUMN `trial_end_at` DATETIME DEFAULT NULL COMMENT '试用到期日' AFTER `expired_at`;

-- 2. PLATFORM 租户无限额
UPDATE `tenant` SET
    `plan_code` = 'PLATFORM',
    `plan_name` = '平台版',
    `max_users` = NULL,
    `max_technicians` = NULL,
    `ticket_monthly_limit` = NULL
WHERE `tenant_code` = 'PLATFORM';

-- 3. 现有非 PLATFORM 租户 → LEGACY 预设（高额度，不低于当前实际用量）
--    maxKnowledgeBases / maxDocuments / maxAiDailyCalls 保持原值不动
UPDATE `tenant` SET
    `plan_code` = 'LEGACY',
    `plan_name` = '历史版',
    `max_users` = 100,
    `max_technicians` = 100,
    `ticket_monthly_limit` = NULL
WHERE `tenant_code` != 'PLATFORM';

-- 4. INACTIVE → SUSPENDED（语义修正）
UPDATE `tenant` SET `status` = 'SUSPENDED' WHERE `status` = 'INACTIVE';
