package com.repair.ai.saas.module.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.repair.ai.saas.module.knowledge.entity.KnowledgeItem;
import com.repair.ai.saas.module.knowledge.mapper.KnowledgeItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * FAQ 轻量检索服务。
 * 按 productType + faultType + 关键词进行 LIKE 查询，最多返回 Top 5 条。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FaqSearchService {

    private final KnowledgeItemMapper knowledgeItemMapper;

    private static final int MAX_RESULTS = 5;

    /**
     * 检索当前租户的 ACTIVE FAQ 条目。
     * 策略：
     *   1. 按 productType + faultType + question 关键词匹配
     *   2. 如果为空，只按 question 关键词匹配
     *   3. 最多返回 5 条
     */
    public List<KnowledgeItem> search(Long tenantId, String question,
                                      String productType, String faultType) {
        // 策略 1：按产品类型 + 故障类型 + 关键词
        if (productType != null && !productType.isBlank()
                && faultType != null && !faultType.isBlank()) {
            List<KnowledgeItem> results = queryWithFilters(tenantId, question, productType, faultType);
            if (!results.isEmpty()) {
                log.debug("FAQ search strategy 1 matched {} items", results.size());
                return results;
            }
        }

        // 策略 2：只按关键词
        List<KnowledgeItem> results = queryWithKeywordOnly(tenantId, question);
        log.debug("FAQ search strategy 2 matched {} items", results.size());
        return results;
    }

    private List<KnowledgeItem> queryWithFilters(Long tenantId, String question,
                                                  String productType, String faultType) {
        LambdaQueryWrapper<KnowledgeItem> wrapper = new LambdaQueryWrapper<KnowledgeItem>()
                .eq(KnowledgeItem::getTenantId, tenantId)
                .eq(KnowledgeItem::getStatus, "ACTIVE")
                .eq(KnowledgeItem::getProductType, productType)
                .eq(KnowledgeItem::getFaultType, faultType);
        addKeywordFilters(wrapper, question);
        wrapper.orderByAsc(KnowledgeItem::getSortOrder)
                .last("LIMIT " + MAX_RESULTS);
        return knowledgeItemMapper.selectList(wrapper);
    }

    private List<KnowledgeItem> queryWithKeywordOnly(Long tenantId, String question) {
        LambdaQueryWrapper<KnowledgeItem> wrapper = new LambdaQueryWrapper<KnowledgeItem>()
                .eq(KnowledgeItem::getTenantId, tenantId)
                .eq(KnowledgeItem::getStatus, "ACTIVE");
        addKeywordFilters(wrapper, question);
        wrapper.orderByAsc(KnowledgeItem::getSortOrder)
                .last("LIMIT " + MAX_RESULTS);
        return knowledgeItemMapper.selectList(wrapper);
    }

    private void addKeywordFilters(LambdaQueryWrapper<KnowledgeItem> wrapper, String question) {
        if (question == null || question.isBlank()) return;
        String[] keywords = extractKeywords(question);
        if (keywords.length == 0) return;

        // 使用 nested() 保证每个关键词组生成 (field1 LIKE ? OR field2 LIKE ? ...)
        // 组间用 AND 连接：(kw1组) AND (kw2组) AND ...
        wrapper.and(w -> {
            for (int i = 0; i < keywords.length; i++) {
                String kw = keywords[i];
                if (i > 0) w.or();
                w.nested(w2 -> w2
                        .like(KnowledgeItem::getQuestion, kw)
                        .or().like(KnowledgeItem::getTitle, kw)
                        .or().like(KnowledgeItem::getKeywords, kw)
                        .or().like(KnowledgeItem::getAnswer, kw)
                );
            }
        });
    }

    /**
     * 从问题中提取关键词（简单分词：按空格和标点拆分，取长度 >= 2 的词）。
     */
    private String[] extractKeywords(String question) {
        if (question == null || question.isBlank()) return new String[0];
        String[] tokens = question.split("[\\s,;.!?，。；！？、]+");
        List<String> keywords = new ArrayList<>();
        for (String token : tokens) {
            String t = token.trim();
            if (t.length() >= 2) {
                keywords.add(t);
            }
        }
        // 如果没有提取到足够长的关键词，用原始问题整体做 LIKE
        if (keywords.isEmpty() && question.trim().length() >= 2) {
            keywords.add(question.trim());
        }
        return keywords.toArray(new String[0]);
    }
}
