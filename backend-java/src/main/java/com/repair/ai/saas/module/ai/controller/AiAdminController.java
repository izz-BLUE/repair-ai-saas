package com.repair.ai.saas.module.ai.controller;

import com.repair.ai.saas.common.ApiResponse;
import com.repair.ai.saas.common.ResultCode;
import com.repair.ai.saas.common.BusinessException;
import com.repair.ai.saas.module.ai.entity.AiConversation;
import com.repair.ai.saas.module.ai.entity.AiMessage;
import com.repair.ai.saas.module.ai.service.AiService;
import com.repair.ai.saas.security.CurrentUser;
import com.repair.ai.saas.security.CurrentUserInfo;
import com.repair.ai.saas.security.RoleChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI 对话管理接口（需 ADMIN / DISPATCHER）。
 */
@RestController
@RequestMapping("/api/admin/ai")
@RequiredArgsConstructor
public class AiAdminController {

    private final AiService aiService;

    @GetMapping("/conversations")
    public ApiResponse<Map<String, Object>> listConversations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @CurrentUserInfo CurrentUser currentUser) {
        RoleChecker.requireAdminOrDispatcher(currentUser);
        // 基础分页限制
        if (page < 1) page = 1;
        if (size < 1) size = 1;
        if (size > 100) size = 100;
        var result = aiService.listConversations(currentUser.getTenantId(), page, size);
        return ApiResponse.success(Map.of(
                "total", result.getTotal(),
                "page", result.getCurrent(),
                "size", result.getSize(),
                "records", result.getRecords()
        ));
    }

    @GetMapping("/conversations/{id}")
    public ApiResponse<Map<String, Object>> getConversation(
            @PathVariable Long id,
            @CurrentUserInfo CurrentUser currentUser) {
        RoleChecker.requireAdminOrDispatcher(currentUser);
        AiConversation conv = aiService.getConversationById(currentUser.getTenantId(), id);
        if (conv == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "对话记录不存在");
        }
        List<AiMessage> messages = aiService.getMessages(currentUser.getTenantId(), id);
        return ApiResponse.success(Map.of(
                "conversation", conv,
                "messages", messages
        ));
    }
}
