package com.repair.ai.saas.module.ai.controller;

import com.repair.ai.saas.common.ApiResponse;
import com.repair.ai.saas.common.BusinessException;
import com.repair.ai.saas.common.ResultCode;
import com.repair.ai.saas.module.ai.service.AiService;
import com.repair.ai.saas.module.ai.service.AiService.AiChatResult;
import com.repair.ai.saas.module.tenant.entity.Tenant;
import com.repair.ai.saas.module.tenant.service.TenantService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI 问答公开接口（无需登录）。
 */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class AiPublicController {

    private final AiService aiService;
    private final TenantService tenantService;

    @PostMapping("/{tenantCode}/ai/chat")
    public ApiResponse<Map<String, Object>> chat(
            @PathVariable String tenantCode,
            @Valid @RequestBody AiChatRequest req) {
        // 根据 tenantCode 找租户
        Tenant tenant = tenantService.getByTenantCode(tenantCode);

        // 校验租户状态和门户启用状态
        if (!"ACTIVE".equals(tenant.getStatus())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "该企业服务暂不可用");
        }
        if (!Boolean.TRUE.equals(tenant.getPortalEnabled())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "该企业服务门户暂未启用");
        }

        AiChatResult result = aiService.chat(
                tenant.getId(),
                req.question,
                req.customerName,
                req.customerPhone,
                req.productType,
                req.faultType,
                "PUBLIC_CHAT"
        );

        return ApiResponse.success(Map.of(
                "answer", result.answer(),
                "shouldCreateTicket", result.shouldCreateTicket(),
                "matchedItemCount", result.matchedItemCount(),
                "conversationId", result.conversationId(),
                "traceId", result.traceId()
        ));
    }

    public record AiChatRequest(
            @NotBlank(message = "问题不能为空") String question,
            String customerName,
            String customerPhone,
            String productType,
            String faultType
    ) {}
}
