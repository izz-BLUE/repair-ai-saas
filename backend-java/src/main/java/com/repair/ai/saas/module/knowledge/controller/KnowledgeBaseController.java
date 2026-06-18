package com.repair.ai.saas.module.knowledge.controller;

import com.repair.ai.saas.common.ApiResponse;
import com.repair.ai.saas.module.knowledge.entity.KnowledgeBase;
import com.repair.ai.saas.module.knowledge.service.KnowledgeBaseService;
import com.repair.ai.saas.module.operation.enums.OperationType;
import com.repair.ai.saas.module.operation.service.OperationLogService;
import com.repair.ai.saas.security.CurrentUser;
import com.repair.ai.saas.security.CurrentUserInfo;
import com.repair.ai.saas.security.RoleChecker;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/knowledge-bases")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final OperationLogService operationLogService;

    @PostMapping
    public ApiResponse<KnowledgeBase> create(@RequestBody CreateKnowledgeBaseRequest req,
                                             @CurrentUserInfo CurrentUser currentUser,
                                             HttpServletRequest request) {
        RoleChecker.requireAdminOrDispatcher(currentUser);
        KnowledgeBase kb = knowledgeBaseService.create(
                currentUser.getTenantId(), req.name(), req.description());
        operationLogService.record(currentUser.getTenantId(), currentUser.getUserId(),
                currentUser.getUsername(), "CREATE_KNOWLEDGE_BASE", "KNOWLEDGE_BASE",
                String.valueOf(kb.getId()), "创建知识库: " + kb.getName(), request.getRemoteAddr());
        return ApiResponse.success(kb);
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @CurrentUserInfo CurrentUser currentUser) {
        RoleChecker.requireAdminOrDispatcher(currentUser);
        var result = knowledgeBaseService.list(currentUser.getTenantId(), page, size, keyword);
        return ApiResponse.success(Map.of(
                "total", result.getTotal(),
                "page", result.getCurrent(),
                "size", result.getSize(),
                "records", result.getRecords()
        ));
    }

    @GetMapping("/{id}")
    public ApiResponse<KnowledgeBase> get(@PathVariable Long id,
                                          @CurrentUserInfo CurrentUser currentUser) {
        RoleChecker.requireAdminOrDispatcher(currentUser);
        return ApiResponse.success(knowledgeBaseService.getById(currentUser.getTenantId(), id));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id,
                                    @RequestBody UpdateKnowledgeBaseRequest req,
                                    @CurrentUserInfo CurrentUser currentUser,
                                    HttpServletRequest request) {
        RoleChecker.requireAdminOrDispatcher(currentUser);
        knowledgeBaseService.update(currentUser.getTenantId(), id, req.name(), req.description());
        operationLogService.record(currentUser.getTenantId(), currentUser.getUserId(),
                currentUser.getUsername(), "UPDATE_KNOWLEDGE_BASE", "KNOWLEDGE_BASE",
                String.valueOf(id), "编辑知识库", request.getRemoteAddr());
        return ApiResponse.success();
    }

    @PutMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id,
                                          @RequestBody UpdateStatusRequest req,
                                          @CurrentUserInfo CurrentUser currentUser,
                                          HttpServletRequest request) {
        RoleChecker.requireAdminOrDispatcher(currentUser);
        knowledgeBaseService.updateStatus(currentUser.getTenantId(), id, req.status());
        operationLogService.record(currentUser.getTenantId(), currentUser.getUserId(),
                currentUser.getUsername(), "UPDATE_KNOWLEDGE_BASE_STATUS", "KNOWLEDGE_BASE",
                String.valueOf(id), "修改知识库状态: " + req.status(), request.getRemoteAddr());
        return ApiResponse.success();
    }

    public record CreateKnowledgeBaseRequest(String name, String description) {}
    public record UpdateKnowledgeBaseRequest(String name, String description) {}
    public record UpdateStatusRequest(String status) {}
}
