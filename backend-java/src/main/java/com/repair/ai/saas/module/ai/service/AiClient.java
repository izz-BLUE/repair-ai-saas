package com.repair.ai.saas.module.ai.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
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
                    @Value("${ai.agent.url:http://localhost:8090}") String agentUrl) {
        this.restTemplate = restTemplate;
        this.agentUrl = agentUrl;
    }

    /**
     * 调用 Python /agent/chat 接口。
     * 返回 null 表示服务不可用或调用失败。
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

    // ---------- 请求/响应 DTO ----------

    @Data
    public static class AgentChatRequest {
        private String question;
        private List<FaqContext> contexts;
        private Long tenantId;
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
        private String traceId;
    }
}
