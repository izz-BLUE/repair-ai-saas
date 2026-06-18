package com.repair.ai.saas.module.ticket.controller;

import com.repair.ai.saas.common.ApiResponse;
import com.repair.ai.saas.common.BusinessException;
import com.repair.ai.saas.common.ResultCode;
import com.repair.ai.saas.module.operation.enums.OperationType;
import com.repair.ai.saas.module.operation.service.OperationLogService;
import com.repair.ai.saas.module.tenant.service.TenantService;
import com.repair.ai.saas.module.ticket.entity.RepairTicket;
import com.repair.ai.saas.module.ticket.entity.TicketStatusLog;
import com.repair.ai.saas.module.ticket.service.TicketService;
import com.repair.ai.saas.security.CurrentUser;
import com.repair.ai.saas.security.CurrentUserInfo;
import com.repair.ai.saas.security.RoleChecker;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;
    private final TenantService tenantService;
    private final OperationLogService operationLogService;

    // ==================== 管理后台接口 ====================

    @GetMapping("/api/admin/tickets")
    public ApiResponse<Map<String, Object>> listTickets(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Long technicianId,
            @RequestParam(required = false) String keyword,
            @CurrentUserInfo CurrentUser currentUser) {
        RoleChecker.requireAdminOrDispatcher(currentUser);
        var result = ticketService.listTickets(currentUser.getTenantId(), page, size,
                status, priority, technicianId, keyword);
        return ApiResponse.success(Map.of(
                "total", result.getTotal(),
                "page", result.getCurrent(),
                "size", result.getSize(),
                "records", result.getRecords()
        ));
    }

    @GetMapping("/api/admin/tickets/{id}")
    public ApiResponse<Map<String, Object>> getTicket(@PathVariable Long id,
                                                       @CurrentUserInfo CurrentUser currentUser) {
        RoleChecker.requireAdminOrDispatcher(currentUser);
        RepairTicket ticket = ticketService.getTicketById(currentUser.getTenantId(), id);
        List<TicketStatusLog> logs = ticketService.getStatusLogs(currentUser.getTenantId(), id);
        return ApiResponse.success(Map.of(
                "ticket", ticket,
                "statusLogs", logs
        ));
    }

    @PostMapping("/api/admin/tickets")
    public ApiResponse<Map<String, Object>> createTicket(@Valid @RequestBody CreateTicketRequest req,
                                                          @CurrentUserInfo CurrentUser currentUser,
                                                          HttpServletRequest request) {
        RoleChecker.requireAdminOrDispatcher(currentUser);
        RepairTicket ticket = ticketService.createTicket(currentUser.getTenantId(),
                req.customerId, req.productType, req.faultType, req.faultDescription,
                req.priority, req.scheduledTime);
        operationLogService.record(currentUser.getTenantId(), currentUser.getUserId(),
                currentUser.getUsername(), OperationType.CREATE_TICKET.name(), "TICKET",
                String.valueOf(ticket.getId()), "创建工单: " + ticket.getTicketNo(), request.getRemoteAddr());
        return ApiResponse.success(Map.of(
                "id", ticket.getId(),
                "ticketNo", ticket.getTicketNo(),
                "status", ticket.getStatus()
        ));
    }

    @PutMapping("/api/admin/tickets/{id}")
    public ApiResponse<Void> updateTicket(@PathVariable Long id,
                                           @RequestBody UpdateTicketRequest req,
                                           @CurrentUserInfo CurrentUser currentUser) {
        RoleChecker.requireAdminOrDispatcher(currentUser);
        ticketService.updateTicket(currentUser.getTenantId(), id,
                req.productType, req.faultType, req.faultDescription, req.priority, req.scheduledTime);
        return ApiResponse.success();
    }

    @PutMapping("/api/admin/tickets/{id}/assign")
    public ApiResponse<Void> assignTicket(@PathVariable Long id,
                                           @Valid @RequestBody AssignRequest req,
                                           @CurrentUserInfo CurrentUser currentUser,
                                           HttpServletRequest request) {
        RoleChecker.requireAdminOrDispatcher(currentUser);
        ticketService.assignTicket(currentUser.getTenantId(), id,
                req.technicianId, req.scheduledTime, currentUser.getUserId());
        operationLogService.record(currentUser.getTenantId(), currentUser.getUserId(),
                currentUser.getUsername(), OperationType.ASSIGN.name(), "TICKET",
                String.valueOf(id), "派单", request.getRemoteAddr());
        return ApiResponse.success();
    }

    @PutMapping("/api/admin/tickets/{id}/reassign")
    public ApiResponse<Void> reassignTicket(@PathVariable Long id,
                                             @Valid @RequestBody AssignRequest req,
                                             @CurrentUserInfo CurrentUser currentUser,
                                             HttpServletRequest request) {
        RoleChecker.requireAdminOrDispatcher(currentUser);
        ticketService.reassignTicket(currentUser.getTenantId(), id,
                req.technicianId, currentUser.getUserId());
        operationLogService.record(currentUser.getTenantId(), currentUser.getUserId(),
                currentUser.getUsername(), OperationType.REASSIGN.name(), "TICKET",
                String.valueOf(id), "改派", request.getRemoteAddr());
        return ApiResponse.success();
    }

    @PutMapping("/api/admin/tickets/{id}/cancel")
    public ApiResponse<Void> cancelTicket(@PathVariable Long id,
                                           @RequestBody(required = false) Map<String, String> body,
                                           @CurrentUserInfo CurrentUser currentUser,
                                           HttpServletRequest request) {
        RoleChecker.requireAdminOrDispatcher(currentUser);
        String remark = body != null ? body.get("remark") : null;
        ticketService.cancelTicket(currentUser.getTenantId(), id, currentUser.getUserId(), remark);
        operationLogService.record(currentUser.getTenantId(), currentUser.getUserId(),
                currentUser.getUsername(), OperationType.CANCEL.name(), "TICKET",
                String.valueOf(id), "取消工单", request.getRemoteAddr());
        return ApiResponse.success();
    }

    @PutMapping("/api/admin/tickets/{id}/close")
    public ApiResponse<Void> closeTicket(@PathVariable Long id,
                                          @CurrentUserInfo CurrentUser currentUser,
                                          HttpServletRequest request) {
        RoleChecker.requireAdminOrDispatcher(currentUser);
        ticketService.closeTicket(currentUser.getTenantId(), id, currentUser.getUserId());
        operationLogService.record(currentUser.getTenantId(), currentUser.getUserId(),
                currentUser.getUsername(), OperationType.CLOSE.name(), "TICKET",
                String.valueOf(id), "关闭工单", request.getRemoteAddr());
        return ApiResponse.success();
    }

    @GetMapping("/api/admin/tickets/{id}/status-logs")
    public ApiResponse<List<TicketStatusLog>> getStatusLogs(@PathVariable Long id,
                                                             @CurrentUserInfo CurrentUser currentUser) {
        RoleChecker.requireAdminOrDispatcher(currentUser);
        return ApiResponse.success(ticketService.getStatusLogs(currentUser.getTenantId(), id));
    }

    // ==================== 公开报修接口 ====================

    @PostMapping("/api/public/{tenantCode}/repair-requests")
    public ApiResponse<Map<String, Object>> publicRepair(
            @PathVariable String tenantCode,
            @Valid @RequestBody RepairRequest req) {
        var tenant = tenantService.getByTenantCode(tenantCode);
        RepairTicket ticket = ticketService.publicRepairRequest(tenant.getId(),
                req.name, req.phone, req.address, req.productType, req.faultDescription);
        return ApiResponse.success(Map.of(
                "ticketNo", ticket.getTicketNo(),
                "status", ticket.getStatus(),
                "message", "报修已提交，请妥善保管工单编号以便查询进度"
        ));
    }

    // ==================== 师傅端接口 ====================

    @GetMapping("/api/technician/tickets")
    public ApiResponse<Map<String, Object>> listMyTickets(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @CurrentUserInfo CurrentUser currentUser) {
        RoleChecker.requireTechnician(currentUser);
        var result = ticketService.listMyTickets(currentUser.getTenantId(),
                currentUser.getUserId(), page, size, status);
        return ApiResponse.success(Map.of(
                "total", result.getTotal(),
                "page", result.getCurrent(),
                "size", result.getSize(),
                "records", result.getRecords()
        ));
    }

    @GetMapping("/api/technician/tickets/{id}")
    public ApiResponse<Map<String, Object>> getMyTicket(@PathVariable Long id,
                                                         @CurrentUserInfo CurrentUser currentUser) {
        RoleChecker.requireTechnician(currentUser);
        RepairTicket ticket = ticketService.getTicketById(currentUser.getTenantId(), id);
        // 校验是否分配给自己
        if (ticket.getTechnicianId() == null || !ticket.getTechnicianId().equals(currentUser.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "该工单未分配给您");
        }
        List<TicketStatusLog> logs = ticketService.getStatusLogs(currentUser.getTenantId(), id);
        return ApiResponse.success(Map.of(
                "ticket", ticket,
                "statusLogs", logs
        ));
    }

    @PutMapping("/api/technician/tickets/{id}/start")
    public ApiResponse<Void> startProcess(@PathVariable Long id,
                                           @CurrentUserInfo CurrentUser currentUser,
                                           HttpServletRequest request) {
        RoleChecker.requireTechnician(currentUser);
        ticketService.startProcess(currentUser.getTenantId(), id, currentUser.getUserId());
        operationLogService.record(currentUser.getTenantId(), currentUser.getUserId(),
                currentUser.getUsername(), OperationType.START_PROCESS.name(), "TICKET",
                String.valueOf(id), "开始处理工单", request.getRemoteAddr());
        return ApiResponse.success();
    }

    @PutMapping("/api/technician/tickets/{id}/complete")
    public ApiResponse<Void> completeTicket(@PathVariable Long id,
                                             @Valid @RequestBody CompleteRequest req,
                                             @CurrentUserInfo CurrentUser currentUser,
                                             HttpServletRequest request) {
        RoleChecker.requireTechnician(currentUser);
        ticketService.completeTicket(currentUser.getTenantId(), id,
                currentUser.getUserId(), req.repairResult, req.costNote, req.partsNote);
        operationLogService.record(currentUser.getTenantId(), currentUser.getUserId(),
                currentUser.getUsername(), OperationType.COMPLETE.name(), "TICKET",
                String.valueOf(id), "提交维修完成", request.getRemoteAddr());
        return ApiResponse.success();
    }

    // ==================== DTO ====================

    public record CreateTicketRequest(
            @NotNull Long customerId,
            String productType,
            String faultType,
            String faultDescription,
            String priority,
            LocalDateTime scheduledTime
    ) {}

    public record UpdateTicketRequest(
            String productType,
            String faultType,
            String faultDescription,
            String priority,
            LocalDateTime scheduledTime
    ) {}

    public record AssignRequest(
            @NotNull Long technicianId,
            LocalDateTime scheduledTime
    ) {}

    public record RepairRequest(
            @NotBlank String name,
            @NotBlank String phone,
            String address,
            String productType,
            @NotBlank String faultDescription
    ) {}

    public record CompleteRequest(
            @NotBlank String repairResult,
            String costNote,
            String partsNote
    ) {}
}
