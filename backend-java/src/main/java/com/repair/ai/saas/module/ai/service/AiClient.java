package com.repair.ai.saas.module.ai.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * 调用 Python AI 服务的 HTTP 客户端。
 * Python 不可用时返回 null，由调用方做兜底处理。
 */
@Slf4j
@Component
public class AiClient {

    private final RestTemplate restTemplate;
    private final String agentUrl;

    public AiClient(RestTemplate restTemplate,
                    @Value("${app.ai.agent.url:http://localhost:8090}") String agentUrl) {
        this.restTemplate = restTemplate;
        this.agentUrl = agentUrl;
    }

    // ==================== Chat ====================

    /**
     * 调用 Python /agent/chat 接口（V0.2.2 向量检索模式）。
     * Java 传 tenantId + question，Python 自己检索 Qdrant + 调用 LLM。
     * 返回 null 表示服务不可用或调用失败。
     */
    public AgentChatResponse chatWithVectorSearch(String question, Long tenantId,
                                                   String productType, String faultType,
                                                   int topK, String traceId) {
        try {
            AgentChatRequest request = new AgentChatRequest();
            request.setQuestion(question);
            request.setTenantId(tenantId);
            request.setProductType(productType);
            request.setFaultType(faultType);
            request.setTopK(topK);
            request.setTraceId(traceId);

            var response = restTemplate.postForEntity(
                    agentUrl + "/agent/chat", request, AgentChatResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
            log.warn("AI service returned non-2xx: {}", response.getStatusCode());
            return null;
        } catch (Exception e) {
            log.warn("AI service unavailable: {}", e.getMessage());
            return null;
        }
    }

    /**
     * V0.2.1 兼容方法：传 contexts 给 Python。
     * V0.2.2 不再使用此方法，保留以备降级场景。
     */
    public AgentChatResponse chat(String question, List<FaqContext> contexts,
                                  Long tenantId, String traceId) {
        try {
            AgentChatRequest request = new AgentChatRequest();
            request.setQuestion(question);
            request.setContexts(contexts);
            request.setTenantId(tenantId);
            request.setTraceId(traceId);

            var response = restTemplate.postForEntity(
                    agentUrl + "/agent/chat", request, AgentChatResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
            log.warn("AI service returned non-2xx: {}", response.getStatusCode());
            return null;
        } catch (Exception e) {
            log.warn("AI service unavailable: {}", e.getMessage());
            return null;
        }
    }

    // ==================== Knowledge Sync / Delete ====================

    /**
     * 同步知识条目到向量库。
     * 失败时返回 null，由调用方决定是否降级。
     */
    public AgentSyncResponse syncKnowledgeItem(Long tenantId, Long itemId, Long knowledgeBaseId,
                                                String title, String question, String answer,
                                                String productType, String faultType, String status) {
        try {
            SyncItemRequest request = new SyncItemRequest();
            request.setTenantId(tenantId);
            request.setItemId(itemId);
            request.setKnowledgeBaseId(knowledgeBaseId);
            request.setTitle(title);
            request.setQuestion(question);
            request.setAnswer(answer);
            request.setProductType(productType);
            request.setFaultType(faultType);
            request.setStatus(status);

            var response = restTemplate.postForEntity(
                    agentUrl + "/agent/knowledge/sync", request, AgentSyncResponse.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
            return null;
        } catch (Exception e) {
            log.warn("AI service sync unavailable: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从向量库删除知识条目。
     */
    public AgentSyncResponse deleteKnowledgeItem(Long tenantId, Long itemId) {
        try {
            DeleteItemRequest request = new DeleteItemRequest();
            request.setTenantId(tenantId);
            request.setItemId(itemId);

            var response = restTemplate.postForEntity(
                    agentUrl + "/agent/knowledge/delete", request, AgentSyncResponse.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
            return null;
        } catch (Exception e) {
            log.warn("AI service delete unavailable: {}", e.getMessage());
            return null;
        }
    }

    // ==================== 请求/响应 DTO ====================

    @Data
    public static class AgentChatRequest {
        private String question;
        private List<FaqContext> contexts;  // 保留，向后兼容
        private Long tenantId;
        private String productType;
        private String faultType;
        private Integer topK;
        private String traceId;
    }

    @Data
    public static class FaqContext {
        private String title;
        private String question;
        private String answer;
    }

    @Data
    public static class AgentChatResponse {
        private String answer;
        private String model;
        @JsonProperty("shouldCreateTicket")
        private boolean shouldCreateTicket;
        private int matchedItemCount;
        private String traceId;
    }

    @Data
    public static class SyncItemRequest {
        private Long tenantId;
        private Long itemId;
        private Long knowledgeBaseId;
        private String title;
        private String question;
        private String answer;
        private String productType;
        private String faultType;
        private String status;
    }

    @Data
    public static class DeleteItemRequest {
        private Long tenantId;
        private Long itemId;
    }

    @Data
    public static class AgentSyncResponse {
        private boolean success;
    }
}
