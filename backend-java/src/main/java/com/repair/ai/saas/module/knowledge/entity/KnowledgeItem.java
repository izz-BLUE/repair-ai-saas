package com.repair.ai.saas.module.knowledge.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("knowledge_item")
public class KnowledgeItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long knowledgeBaseId;

    private String title;

    private String question;

    private String answer;

    private String productType;

    private String faultType;

    private String keywords;

    private Integer sortOrder;

    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
