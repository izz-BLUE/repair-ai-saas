package com.repair.ai.saas.module.ticket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.repair.ai.saas.common.BusinessException;
import com.repair.ai.saas.common.ResultCode;
import com.repair.ai.saas.module.customer.entity.Customer;
import com.repair.ai.saas.module.customer.service.CustomerService;
import com.repair.ai.saas.module.ticket.entity.RepairTicket;
import com.repair.ai.saas.module.ticket.entity.TicketStatusLog;
import com.repair.ai.saas.module.ticket.enums.TicketPriority;
import com.repair.ai.saas.module.ticket.enums.TicketStatus;
import com.repair.ai.saas.module.ticket.mapper.RepairTicketMapper;
import com.repair.ai.saas.module.ticket.mapper.TicketStatusLogMapper;
import com.repair.ai.saas.module.user.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final RepairTicketMapper ticketMapper;
    private final TicketStatusLogMapper statusLogMapper;
    private final CustomerService customerService;
    private final SysUserService sysUserService;
    private final StringRedisTemplate redisTemplate;

    // ---------- 工单编号生成 ----------

    private String generateTicketNo(Long tenantId) {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String redisKey = "ticket_no:" + tenantId + ":" + dateStr;
        Long seq = redisTemplate.opsForValue().increment(redisKey);
        // 首次设置时给 48h TTL
        if (seq != null && seq == 1) {
            redisTemplate.expire(redisKey, java.time.Duration.ofHours(48));
        }
        // 格式: TK + 日期 + 4位序号
        return "TK" + dateStr + String.format("%04d", seq != null ? seq : 1);
    }

    // ---------- 创建工单 ----------

    @Transactional
    public RepairTicket createTicket(Long tenantId, Long customerId,
                                     String productType, String faultType,
                                     String faultDescription, String priority,
                                     LocalDateTime scheduledTime) {
        // 优先级校验
        String normalizedPriority = "NORMAL";
        if (priority != null && !priority.isBlank()) {
            TicketPriority p = TicketPriority.fromString(priority);
            if (p == null) {
                throw new BusinessException(ResultCode.VALIDATION_ERROR,
                        "无效的优先级: " + priority + "，可选值: LOW/NORMAL/HIGH/URGENT");
            }
            normalizedPriority = p.name();
        }

        Customer customer = customerService.getCustomerById(tenantId, customerId);

        RepairTicket ticket = new RepairTicket();
        ticket.setTenantId(tenantId);
        ticket.setTicketNo(generateTicketNo(tenantId));
        ticket.setCustomerId(customer.getId());
        ticket.setCustomerName(customer.getName());
        ticket.setCustomerPhone(customer.getPhone());
        ticket.setServiceAddress(customer.getAddress());
        ticket.setProductType(productType);
        ticket.setFaultType(faultType);
        ticket.setFaultDescription(faultDescription);
        ticket.setPriority(normalizedPriority);
        ticket.setStatus(TicketStatus.PENDING.name());
        ticket.setScheduledTime(scheduledTime);
        ticketMapper.insert(ticket);

        // 状态日志
        writeStatusLog(tenantId, ticket.getId(), null, TicketStatus.PENDING.name(), null, "创建工单");

        return ticket;
    }

    // ---------- 公开报修 ----------

    @Transactional
    public RepairTicket publicRepairRequest(Long tenantId, String name, String phone,
                                            String address, String productType,
                                            String faultDescription) {
        // 自动创建/合并客户
        Customer customer = customerService.createOrGetCustomer(tenantId, name, phone, address, null);

        RepairTicket ticket = new RepairTicket();
        ticket.setTenantId(tenantId);
        ticket.setTicketNo(generateTicketNo(tenantId));
        ticket.setCustomerId(customer.getId());
        ticket.setCustomerName(customer.getName());
        ticket.setCustomerPhone(customer.getPhone());
        ticket.setServiceAddress(address != null ? address : customer.getAddress());
        ticket.setProductType(productType);
        ticket.setFaultDescription(faultDescription);
        ticket.setPriority("NORMAL");
        ticket.setStatus(TicketStatus.PENDING.name());
        ticketMapper.insert(ticket);

        writeStatusLog(tenantId, ticket.getId(), null, TicketStatus.PENDING.name(), null, "客户报修");

        return ticket;
    }

    // ---------- 工单查询 ----------

    public Page<RepairTicket> listTickets(Long tenantId, int page, int size,
                                          String status, String priority,
                                          Long technicianId, String keyword) {
        Page<RepairTicket> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<RepairTicket> wrapper = new LambdaQueryWrapper<RepairTicket>()
                .eq(RepairTicket::getTenantId, tenantId);
        if (status != null && !status.isBlank()) {
            TicketStatus ts = TicketStatus.fromString(status);
            if (ts == null) {
                throw new BusinessException(ResultCode.VALIDATION_ERROR,
                        "无效的工单状态: " + status);
            }
            wrapper.eq(RepairTicket::getStatus, ts.name());
        }
        if (priority != null && !priority.isBlank()) {
            TicketPriority p = TicketPriority.fromString(priority);
            if (p == null) {
                throw new BusinessException(ResultCode.VALIDATION_ERROR,
                        "无效的优先级: " + priority);
            }
            wrapper.eq(RepairTicket::getPriority, p.name());
        }
        if (technicianId != null) {
            wrapper.eq(RepairTicket::getTechnicianId, technicianId);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(RepairTicket::getTicketNo, keyword)
                    .or().like(RepairTicket::getCustomerName, keyword)
                    .or().like(RepairTicket::getCustomerPhone, keyword));
        }
        wrapper.orderByDesc(RepairTicket::getCreatedAt);
        return ticketMapper.selectPage(pageParam, wrapper);
    }

    public RepairTicket getTicketById(Long tenantId, Long ticketId) {
        RepairTicket ticket = ticketMapper.selectOne(
                new LambdaQueryWrapper<RepairTicket>()
                        .eq(RepairTicket::getTenantId, tenantId)
                        .eq(RepairTicket::getId, ticketId)
        );
        if (ticket == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "工单不存在");
        }
        return ticket;
    }

    public List<TicketStatusLog> getStatusLogs(Long tenantId, Long ticketId) {
        return statusLogMapper.selectList(
                new LambdaQueryWrapper<TicketStatusLog>()
                        .eq(TicketStatusLog::getTenantId, tenantId)
                        .eq(TicketStatusLog::getTicketId, ticketId)
                        .orderByAsc(TicketStatusLog::getCreatedAt)
        );
    }

    // ---------- 编辑工单 ----------

    @Transactional
    public void updateTicket(Long tenantId, Long ticketId,
                             String productType, String faultType,
                             String faultDescription, String priority,
                             LocalDateTime scheduledTime) {
        RepairTicket ticket = getTicketById(tenantId, ticketId);
        if (!TicketStatus.PENDING.name().equals(ticket.getStatus())) {
            throw new BusinessException(ResultCode.INVALID_STATE_TRANSITION, "只有待处理状态的工单可以编辑");
        }
        if (productType != null) ticket.setProductType(productType);
        if (faultType != null) ticket.setFaultType(faultType);
        if (faultDescription != null) ticket.setFaultDescription(faultDescription);
        if (priority != null) {
            TicketPriority p = TicketPriority.fromString(priority);
            if (p == null) {
                throw new BusinessException(ResultCode.VALIDATION_ERROR,
                        "无效的优先级: " + priority + "，可选值: LOW/NORMAL/HIGH/URGENT");
            }
            ticket.setPriority(p.name());
        }
        if (scheduledTime != null) ticket.setScheduledTime(scheduledTime);
        ticketMapper.updateById(ticket);
    }

    // ---------- 派单 ----------

    @Transactional
    public void assignTicket(Long tenantId, Long ticketId, Long technicianId,
                             LocalDateTime scheduledTime, Long operatorId) {
        RepairTicket ticket = getTicketById(tenantId, ticketId);

        // 状态校验
        TicketStatus currentStatus = TicketStatus.valueOf(ticket.getStatus());
        if (!currentStatus.canTransitionTo(TicketStatus.ASSIGNED)) {
            throw new BusinessException(ResultCode.INVALID_STATE_TRANSITION,
                    "当前状态 " + currentStatus.getLabel() + " 不能派单");
        }

        // 校验师傅
        sysUserService.getTechnician(tenantId, technicianId);

        TicketStatus oldStatus = currentStatus;
        ticket.setTechnicianId(technicianId);
        ticket.setStatus(TicketStatus.ASSIGNED.name());
        if (scheduledTime != null) ticket.setScheduledTime(scheduledTime);
        ticketMapper.updateById(ticket);

        writeStatusLog(tenantId, ticketId, oldStatus.name(),
                TicketStatus.ASSIGNED.name(), operatorId, "派单给师傅");
    }

    // ---------- 改派 ----------

    @Transactional
    public void reassignTicket(Long tenantId, Long ticketId, Long newTechnicianId, Long operatorId) {
        RepairTicket ticket = getTicketById(tenantId, ticketId);

        TicketStatus currentStatus = TicketStatus.valueOf(ticket.getStatus());
        if (currentStatus != TicketStatus.ASSIGNED && currentStatus != TicketStatus.IN_PROGRESS) {
            throw new BusinessException(ResultCode.INVALID_STATE_TRANSITION, "只有已派单或处理中的工单可以改派");
        }

        sysUserService.getTechnician(tenantId, newTechnicianId);

        ticket.setTechnicianId(newTechnicianId);
        ticketMapper.updateById(ticket);

        writeStatusLog(tenantId, ticketId, currentStatus.name(),
                currentStatus.name(), operatorId, "改派给另一位师傅");
    }

    // ---------- 师傅开始处理 ----------

    @Transactional
    public void startProcess(Long tenantId, Long ticketId, Long technicianId) {
        RepairTicket ticket = getTicketById(tenantId, ticketId);

        // 权限校验：只能操作分配给自己的工单
        if (ticket.getTechnicianId() == null || !ticket.getTechnicianId().equals(technicianId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "该工单未分配给您");
        }

        TicketStatus currentStatus = TicketStatus.valueOf(ticket.getStatus());
        if (!currentStatus.canTransitionTo(TicketStatus.IN_PROGRESS)) {
            throw new BusinessException(ResultCode.INVALID_STATE_TRANSITION,
                    "当前状态 " + currentStatus.getLabel() + " 不能开始处理");
        }

        ticket.setStatus(TicketStatus.IN_PROGRESS.name());
        ticket.setStartTime(LocalDateTime.now());
        ticketMapper.updateById(ticket);

        writeStatusLog(tenantId, ticketId, currentStatus.name(),
                TicketStatus.IN_PROGRESS.name(), technicianId, "师傅开始处理");
    }

    // ---------- 师傅提交完成 ----------

    @Transactional
    public void completeTicket(Long tenantId, Long ticketId, Long technicianId,
                               String repairResult, String costNote, String partsNote) {
        RepairTicket ticket = getTicketById(tenantId, ticketId);

        if (ticket.getTechnicianId() == null || !ticket.getTechnicianId().equals(technicianId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "该工单未分配给您");
        }

        TicketStatus currentStatus = TicketStatus.valueOf(ticket.getStatus());
        if (!currentStatus.canTransitionTo(TicketStatus.COMPLETED)) {
            throw new BusinessException(ResultCode.INVALID_STATE_TRANSITION,
                    "当前状态 " + currentStatus.getLabel() + " 不能提交完成");
        }

        ticket.setStatus(TicketStatus.COMPLETED.name());
        ticket.setRepairResult(repairResult);
        ticket.setCostNote(costNote);
        ticket.setPartsNote(partsNote);
        ticket.setCompletionTime(LocalDateTime.now());
        ticketMapper.updateById(ticket);

        writeStatusLog(tenantId, ticketId, currentStatus.name(),
                TicketStatus.COMPLETED.name(), technicianId, "维修完成");
    }

    // ---------- 取消工单 ----------

    @Transactional
    public void cancelTicket(Long tenantId, Long ticketId, Long operatorId, String remark) {
        RepairTicket ticket = getTicketById(tenantId, ticketId);

        TicketStatus currentStatus = TicketStatus.valueOf(ticket.getStatus());
        if (!currentStatus.canTransitionTo(TicketStatus.CANCELLED)) {
            throw new BusinessException(ResultCode.INVALID_STATE_TRANSITION,
                    "当前状态 " + currentStatus.getLabel() + " 不能取消");
        }

        ticket.setStatus(TicketStatus.CANCELLED.name());
        ticketMapper.updateById(ticket);

        writeStatusLog(tenantId, ticketId, currentStatus.name(),
                TicketStatus.CANCELLED.name(), operatorId,
                remark != null ? remark : "取消工单");
    }

    // ---------- 关闭工单 ----------

    @Transactional
    public void closeTicket(Long tenantId, Long ticketId, Long operatorId) {
        RepairTicket ticket = getTicketById(tenantId, ticketId);

        TicketStatus currentStatus = TicketStatus.valueOf(ticket.getStatus());
        if (!currentStatus.canTransitionTo(TicketStatus.CLOSED)) {
            throw new BusinessException(ResultCode.INVALID_STATE_TRANSITION,
                    "当前状态 " + currentStatus.getLabel() + " 不能关闭");
        }

        ticket.setStatus(TicketStatus.CLOSED.name());
        ticketMapper.updateById(ticket);

        writeStatusLog(tenantId, ticketId, currentStatus.name(),
                TicketStatus.CLOSED.name(), operatorId, "关闭工单");
    }

    // ---------- 师傅查看自己的工单 ----------

    public Page<RepairTicket> listMyTickets(Long tenantId, Long technicianId, int page, int size, String status) {
        Page<RepairTicket> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<RepairTicket> wrapper = new LambdaQueryWrapper<RepairTicket>()
                .eq(RepairTicket::getTenantId, tenantId)
                .eq(RepairTicket::getTechnicianId, technicianId);
        if (status != null && !status.isBlank()) {
            TicketStatus ts = TicketStatus.fromString(status);
            if (ts == null) {
                throw new BusinessException(ResultCode.VALIDATION_ERROR,
                        "无效的工单状态: " + status);
            }
            wrapper.eq(RepairTicket::getStatus, ts.name());
        }
        wrapper.orderByDesc(RepairTicket::getCreatedAt);
        return ticketMapper.selectPage(pageParam, wrapper);
    }

    // ---------- 内部方法 ----------

    private void writeStatusLog(Long tenantId, Long ticketId, String fromStatus,
                                String toStatus, Long operatorId, String remark) {
        TicketStatusLog log = new TicketStatusLog();
        log.setTenantId(tenantId);
        log.setTicketId(ticketId);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setOperatorId(operatorId);
        log.setRemark(remark);
        statusLogMapper.insert(log);
    }
}
