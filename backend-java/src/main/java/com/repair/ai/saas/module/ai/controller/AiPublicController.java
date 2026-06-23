package com.repair.ai.saas.module.ai.controller;

import com.repair.ai.saas.common.ApiResponse;
import com.repair.ai.saas.common.BusinessException;
import com.repair.ai.saas.common.RateLimiter;
import com.repair.ai.saas.common.ResultCode;
import com.repair.ai.saas.common.TenantAccessChecker;
import com.repair.ai.saas.module.ai.service.AiService;
import com.repair.ai.saas.module.ai.service.AiService.AiChatResult;
import com.repair.ai.saas.module.ai.service.AiUsageService;
import com.repair.ai.saas.module.tenant.entity.Tenant;
import com.repair.ai.saas.module.tenant.service.TenantService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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
    private final AiUsageService aiUsageService;
    private final RateLimiter rateLimiter;

    @PostMapping("/{tenantCode}/ai/chat")
    public ApiResponse<Map<String, Object>> chat(
            @PathVariable String tenantCode,
            @Valid @RequestBody AiChatRequest req,
            HttpServletRequest request) {
        // IP 级限流（防 AI 滥用，租户级日限额由 AiUsageService 负责）
        if (!rateLimiter.checkChatIp(RateLimiter.getClientIp(request))) {
            throw new BusinessException(ResultCode.TOO_MANY_REQUESTS, RateLimiter.RATE_LIMIT_MSG);
        }
        // 根据 tenantCode 找租户
        Tenant tenant = tenantService.getByTenantCode(tenantCode);

        // 试用到期自动转 EXPIRED
        tenantService.autoExpireIfTrialEnded(tenant);

        // 统一租户访问检查（AI 咨询：TRIAL/ACTIVE 可用，EXPIRED 不可用）
        TenantAccessChecker.requireAiAllowed(tenant);

        // 校验门户启用状态
        if (!Boolean.TRUE.equals(tenant.getPortalEnabled())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "该企业服务门户暂未启用");
        }

        // AI 日调用量检查（公开接口受限额约束）
        aiUsageService.checkAndIncrement(tenant.getId(), tenant.getMaxAiDailyCalls());

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
