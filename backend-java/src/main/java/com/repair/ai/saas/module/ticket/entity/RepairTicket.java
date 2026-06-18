package com.repair.ai.saas.module.ticket.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("repair_ticket")
public class RepairTicket {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String ticketNo;

    private Long customerId;

    private String customerName;

    private String customerPhone;

    private String serviceAddress;

    private String productType;

    private String faultType;

    private String faultDescription;

    private String priority;

    private String status;

    private Long technicianId;

    private LocalDateTime scheduledTime;

    private LocalDateTime startTime;

    private LocalDateTime completionTime;

    private String repairResult;

    private String costNote;

    private String partsNote;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
