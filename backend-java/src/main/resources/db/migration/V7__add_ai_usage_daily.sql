-- AI 日调用量统计表
CREATE TABLE IF NOT EXISTS ai_usage_daily (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id   BIGINT       NOT NULL COMMENT '租户 ID',
    usage_date  DATE         NOT NULL COMMENT '统计日期',
    call_count  INT          NOT NULL DEFAULT 0 COMMENT '当日已调用次数',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tenant_date (tenant_id, usage_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI 日调用量统计';
