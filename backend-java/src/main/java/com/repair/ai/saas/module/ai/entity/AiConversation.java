package com.repair.ai.saas.module.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_conversation")
public class AiConversation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String customerPhone;

    private String customerName;

    private String source;

    private String question;

    private String answer;

    private Integer matchedItemCount;

    private Integer shouldCreateTicket;

    private String model;

    private String traceId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
