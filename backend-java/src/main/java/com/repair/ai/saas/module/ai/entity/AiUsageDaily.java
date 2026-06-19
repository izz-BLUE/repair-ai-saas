package com.repair.ai.saas.module.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("ai_usage_daily")
public class AiUsageDaily {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private LocalDate usageDate;

    private Integer callCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
