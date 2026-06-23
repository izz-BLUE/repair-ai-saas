package com.repair.ai.saas.module.tenant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tenant")
public class Tenant {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String tenantCode;

    private String name;

    private String contactName;

    private String contactPhone;

    private String address;

    private String portalTitle;

    private String portalDescription;

    private String logoUrl;

    private String themeColor;

    private Boolean portalEnabled;

    private Integer maxKnowledgeBases;

    private Integer maxDocuments;

    private Integer maxAiDailyCalls;

    private String planCode;

    private String planName;

    private Integer maxUsers;

    private Integer maxTechnicians;

    private Integer ticketMonthlyLimit;

    private LocalDateTime expiredAt;

    private LocalDateTime trialEndAt;

    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
