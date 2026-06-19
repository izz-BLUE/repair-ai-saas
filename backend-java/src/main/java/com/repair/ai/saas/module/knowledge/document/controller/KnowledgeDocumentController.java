package com.repair.ai.saas.module.knowledge.document.controller;

import com.repair.ai.saas.common.ApiResponse;
import com.repair.ai.saas.module.knowledge.document.entity.KnowledgeDocument;
import com.repair.ai.saas.module.knowledge.document.service.KnowledgeDocumentService;
import com.repair.ai.saas.module.operation.service.OperationLogService;
import com.repair.ai.saas.security.CurrentUser;
import com.repair.ai.saas.security.CurrentUserInfo;
import com.repair.ai.saas.security.RoleChecker;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/knowledge-documents")
@RequiredArgsConstructor
public class KnowledgeDocumentController {

    private final KnowledgeDocumentService documentService;
    private final OperationLogService operationLogService;

    /**
     * 上传文档并解析生成知识条目。
     * multipart/form-data: knowledgeBaseId + file
     */
    @PostMapping("/upload")
    public ApiResponse<KnowledgeDocument> upload(
            @RequestParam Long knowledgeBaseId,
            @RequestParam MultipartFile file,
            @CurrentUserInfo CurrentUser currentUser,
            HttpServletRequest request) {
        RoleChecker.requireAdminOrDispatcher(currentUser);

        KnowledgeDocument doc = documentService.upload(
                currentUser.getTenantId(), knowledgeBaseId, file, currentUser.getUserId());

        operationLogService.record(currentUser.getTenantId(), currentUser.getUserId(),
                currentUser.getUsername(), "UPLOAD_KNOWLEDGE_DOCUMENT", "KNOWLEDGE_DOCUMENT",
                String.valueOf(doc.getId()), "上传知识文档: " + doc.getOriginalFilename(),
                request.getRemoteAddr());

        return ApiResponse.success(doc);
    }

    /**
     * 文档列表（分页）。
     */
    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long knowledgeBaseId,
            @RequestParam(required = false) String parseStatus,
            @CurrentUserInfo CurrentUser currentUser) {
        RoleChecker.requireAdminOrDispatcher(currentUser);

        var result = documentService.list(
                currentUser.getTenantId(), page, size, knowledgeBaseId, parseStatus);

        return ApiResponse.success(Map.of(
                "total", result.getTotal(),
                "page", result.getCurrent(),
                "size", result.getSize(),
                "records", result.getRecords()
        ));
    }

    /**
     * 文档详情。
     */
    @GetMapping("/{id}")
    public ApiResponse<KnowledgeDocument> get(
            @PathVariable Long id,
            @CurrentUserInfo CurrentUser currentUser) {
        RoleChecker.requireAdminOrDispatcher(currentUser);
        return ApiResponse.success(documentService.getById(currentUser.getTenantId(), id));
    }

    /**
     * 删除文档（逻辑删除），关联条目标记 INACTIVE。
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @PathVariable Long id,
            @CurrentUserInfo CurrentUser currentUser,
            HttpServletRequest request) {
        RoleChecker.requireAdminOrDispatcher(currentUser);

        documentService.delete(currentUser.getTenantId(), id);

        operationLogService.record(currentUser.getTenantId(), currentUser.getUserId(),
                currentUser.getUsername(), "DELETE_KNOWLEDGE_DOCUMENT", "KNOWLEDGE_DOCUMENT",
                String.valueOf(id), "删除知识文档", request.getRemoteAddr());

        return ApiResponse.success();
    }

    /**
     * 重新解析文档。
     */
    @PostMapping("/{id}/reparse")
    public ApiResponse<KnowledgeDocument> reparse(
            @PathVariable Long id,
            @CurrentUserInfo CurrentUser currentUser,
            HttpServletRequest request) {
        RoleChecker.requireAdminOrDispatcher(currentUser);

        KnowledgeDocument doc = documentService.reparse(currentUser.getTenantId(), id);

        operationLogService.record(currentUser.getTenantId(), currentUser.getUserId(),
                currentUser.getUsername(), "REPARSE_KNOWLEDGE_DOCUMENT", "KNOWLEDGE_DOCUMENT",
                String.valueOf(id), "重新解析知识文档", request.getRemoteAddr());

        return ApiResponse.success(doc);
    }
}
