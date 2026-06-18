package com.repair.ai.saas.module.ai.service;

import com.repair.ai.saas.module.ai.entity.AiConversation;
import com.repair.ai.saas.module.ai.entity.AiMessage;
import com.repair.ai.saas.module.ai.mapper.AiConversationMapper;
import com.repair.ai.saas.module.ai.mapper.AiMessageMapper;
import com.repair.ai.saas.module.ai.service.AiClient.FaqContext;
import com.repair.ai.saas.module.knowledge.entity.KnowledgeItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * AI 问答核心编排服务。
 * 流程：检索 FAQ → 调用 AI（或兜底）→ 落库。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final FaqSearchService faqSearchService;
    private final AiClient aiClient;
    private final AiConversationMapper conversationMapper;
    private final AiMessageMapper messageMapper;

    private static final String FALLBACK_MODEL = "FALLBACK";

    private static final String NO_FAQ_ANSWER = "当前知识库中没有足够信息判断该问题。建议提交报修，由客服或维修师傅进一步确认。";

    @Transactional
    public AiChatResult chat(Long tenantId, String question, String customerName,
                             String customerPhone, String productType, String faultType,
                             String source) {
        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        // 1. 检索 FAQ
        List<KnowledgeItem> matchedItems = faqSearchService.search(
                tenantId, question, productType, faultType);
        int matchedCount = matchedItems.size();

        log.info("AI chat: tenantId={}, question='{}', matchedFAQ={}, traceId={}",
                tenantId, question, matchedCount, traceId);

        String answer;
        String model;
        boolean shouldCreateTicket;

        if (matchedCount == 0) {
            // 2. 无 FAQ 命中 → 不调用 AI
            answer = NO_FAQ_ANSWER;
            model = null;
            shouldCreateTicket = true;
        } else {
            // 3. 有 FAQ 命中 → 调用 Python AI 服务
            List<FaqContext> contexts = matchedItems.stream()
                    .map(item -> {
                        FaqContext ctx = new FaqContext();
                        ctx.setTitle(item.getTitle());
                        ctx.setQuestion(item.getQuestion());
                        ctx.setAnswer(item.getAnswer());
                        return ctx;
                    })
                    .collect(Collectors.toList());

            var aiResponse = aiClient.chat(question, contexts, tenantId, traceId);

            if (aiResponse != null) {
                answer = aiResponse.getAnswer();
                model = aiResponse.getModel();
                shouldCreateTicket = aiResponse.isShouldCreateTicket();
            } else {
                // 4. AI 服务不可用 → 兜底：返回 FAQ 摘要
                answer = buildFallbackAnswer(matchedItems);
                model = FALLBACK_MODEL;
                shouldCreateTicket = false;
            }
        }

        // 5. 落库
        AiConversation conv = new AiConversation();
        conv.setTenantId(tenantId);
        conv.setCustomerPhone(customerPhone);
        conv.setCustomerName(customerName);
        conv.setSource(source);
        conv.setQuestion(question);
        conv.setAnswer(answer);
        conv.setMatchedItemCount(matchedCount);
        conv.setShouldCreateTicket(shouldCreateTicket ? 1 : 0);
        conv.setModel(model);
        conv.setTraceId(traceId);
        conversationMapper.insert(conv);

        // 写入消息记录
        saveMessage(tenantId, conv.getId(), "USER", question);
        saveMessage(tenantId, conv.getId(), "ASSISTANT", answer);

        return new AiChatResult(answer, shouldCreateTicket, matchedCount, conv.getId(), traceId);
    }

    private void saveMessage(Long tenantId, Long conversationId, String role, String content) {
        AiMessage msg = new AiMessage();
        msg.setTenantId(tenantId);
        msg.setConversationId(conversationId);
        msg.setRole(role);
        msg.setContent(content);
        messageMapper.insert(msg);
    }

    /**
     * 当 AI 服务不可用时，用 FAQ 条目拼接兜底回答。
     */
    private String buildFallbackAnswer(List<KnowledgeItem> items) {
        StringBuilder sb = new StringBuilder();
        sb.append("根据知识库信息，以下内容可能与您的问题相关：\n\n");
        for (int i = 0; i < items.size(); i++) {
            KnowledgeItem item = items.get(i);
            sb.append(String.format("%d. %s\n", i + 1, item.getTitle()));
            sb.append(String.format("   问题：%s\n", item.getQuestion()));
            sb.append(String.format("   解答：%s\n\n", item.getAnswer()));
        }
        sb.append("以上为知识库自动匹配结果，如需进一步帮助请联系客服或提交报修。");
        return sb.toString();
    }

    // ---------- 查询接口 ----------

    public AiConversation getConversationById(Long tenantId, Long id) {
        return conversationMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiConversation>()
                        .eq(AiConversation::getTenantId, tenantId)
                        .eq(AiConversation::getId, id)
        );
    }

    public List<AiMessage> getMessages(Long tenantId, Long conversationId) {
        return messageMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiMessage>()
                        .eq(AiMessage::getTenantId, tenantId)
                        .eq(AiMessage::getConversationId, conversationId)
                        .orderByAsc(AiMessage::getCreatedAt)
        );
    }

    public com.baomidou.mybatisplus.extension.plugins.pagination.Page<AiConversation> listConversations(
            Long tenantId, int page, int size) {
        var pageParam = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<AiConversation>(page, size);
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiConversation>()
                .eq(AiConversation::getTenantId, tenantId)
                .orderByDesc(AiConversation::getCreatedAt);
        return conversationMapper.selectPage(pageParam, wrapper);
    }

    // ---------- 结果 DTO ----------

    public record AiChatResult(String answer, boolean shouldCreateTicket,
                                int matchedItemCount, Long conversationId, String traceId) {}
}
