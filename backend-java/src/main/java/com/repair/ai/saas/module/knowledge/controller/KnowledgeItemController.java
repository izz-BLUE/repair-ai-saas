package com.repair.ai.saas.module.knowledge.controller;

import com.repair.ai.saas.common.ApiResponse;
import com.repair.ai.saas.module.knowledge.entity.KnowledgeItem;
import com.repair.ai.saas.module.knowledge.service.KnowledgeItemService;
import com.repair.ai.saas.module.operation.service.OperationLogService;
import com.repair.ai.saas.security.CurrentUser;
import com.repair.ai.saas.security.CurrentUserInfo;
import com.repair.ai.saas.security.RoleChecker;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/knowledge-items")
@RequiredArgsConstructor
public class KnowledgeItemController {

    private final KnowledgeItemService knowledgeItemService;
    private final OperationLogService operationLogService;

    @PostMapping
    public ApiResponse<KnowledgeItem> create(@RequestBody CreateKnowledgeItemRequest req,
                                             @CurrentUserInfo CurrentUser currentUser,
                                             HttpServletRequest request) {
        RoleChecker.requireAdminOrDispatcher(currentUser);
        KnowledgeItem item = knowledgeItemService.create(
                currentUser.getTenantId(), req.knowledgeBaseId(), req.title(),
                req.question(), req.answer(), req.productType(),
                req.faultType(), req.keywords(), req.sortOrder());
        operationLogService.record(currentUser.getTenantId(), currentUser.getUserId(),
                currentUser.getUsername(), "CREATE_KNOWLEDGE_ITEM", "KNOWLEDGE_ITEM",
                String.valueOf(item.getId()), "创建知识条目: " + item.getTitle(), request.getRemoteAddr());
        return ApiResponse.success(item);
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long knowledgeBaseId,
            @RequestParam(required = false) String keyword,
            @CurrentUserInfo CurrentUser currentUser) {
        RoleChecker.requireAdminOrDispatcher(currentUser);
        var result = knowledgeItemService.list(
                currentUser.getTenantId(), page, size, knowledgeBaseId, keyword);
        return ApiResponse.success(Map.of(
                "total", result.getTotal(),
                "page", result.getCurrent(),
                "size", result.getSize(),
                "records", result.getRecords()
        ));
    }

    @GetMapping("/{id}")
    public ApiResponse<KnowledgeItem> get(@PathVariable Long id,
                                          @CurrentUserInfo CurrentUser currentUser) {
        RoleChecker.requireAdminOrDispatcher(currentUser);
        return ApiResponse.success(knowledgeItemService.getById(currentUser.getTenantId(), id));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id,
                                    @RequestBody UpdateKnowledgeItemRequest req,
                                    @CurrentUserInfo CurrentUser currentUser,
                                    HttpServletRequest request) {
        RoleChecker.requireAdminOrDispatcher(currentUser);
        knowledgeItemService.update(currentUser.getTenantId(), id,
                req.knowledgeBaseId(), req.title(), req.question(), req.answer(),
                req.productType(), req.faultType(), req.keywords(), req.sortOrder());
        operationLogService.record(currentUser.getTenantId(), currentUser.getUserId(),
                currentUser.getUsername(), "UPDATE_KNOWLEDGE_ITEM", "KNOWLEDGE_ITEM",
                String.valueOf(id), "编辑知识条目", request.getRemoteAddr());
        return ApiResponse.success();
    }

    @PutMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id,
                                          @RequestBody UpdateStatusRequest req,
                                          @CurrentUserInfo CurrentUser currentUser,
                                          HttpServletRequest request) {
        RoleChecker.requireAdminOrDispatcher(currentUser);
        knowledgeItemService.updateStatus(currentUser.getTenantId(), id, req.status());
        operationLogService.record(currentUser.getTenantId(), currentUser.getUserId(),
                currentUser.getUsername(), "UPDATE_KNOWLEDGE_ITEM_STATUS", "KNOWLEDGE_ITEM",
                String.valueOf(id), "修改知识条目状态: " + req.status(), request.getRemoteAddr());
        return ApiResponse.success();
    }

    public record CreateKnowledgeItemRequest(Long knowledgeBaseId, String title,
                                              String question, String answer,
                                              String productType, String faultType,
                                              String keywords, Integer sortOrder) {}
    public record UpdateKnowledgeItemRequest(Long knowledgeBaseId, String title,
                                              String question, String answer,
                                              String productType, String faultType,
                                              String keywords, Integer sortOrder) {}
    public record UpdateStatusRequest(String status) {}
}
