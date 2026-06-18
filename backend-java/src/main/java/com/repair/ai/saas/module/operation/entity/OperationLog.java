package com.repair.ai.saas.module.operation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("operation_log")
public class OperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long operatorId;

    private String operatorName;

    private String operationType;

    private String targetType;

    private String targetId;

    private String description;

    private String requestIp;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
