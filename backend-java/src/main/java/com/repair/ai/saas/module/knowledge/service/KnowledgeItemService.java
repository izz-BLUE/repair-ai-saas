package com.repair.ai.saas.module.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.repair.ai.saas.common.BusinessException;
import com.repair.ai.saas.common.ResultCode;
import com.repair.ai.saas.module.ai.service.AiClient;
import com.repair.ai.saas.module.knowledge.entity.KnowledgeBase;
import com.repair.ai.saas.module.knowledge.entity.KnowledgeItem;
import com.repair.ai.saas.module.knowledge.enums.KnowledgeStatus;
import com.repair.ai.saas.module.knowledge.mapper.KnowledgeBaseMapper;
import com.repair.ai.saas.module.knowledge.mapper.KnowledgeItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeItemService {

    private final KnowledgeItemMapper knowledgeItemMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final AiClient aiClient;

    public KnowledgeItem create(Long tenantId, Long knowledgeBaseId, String title,
                                String question, String answer, String productType,
                                String faultType, String keywords, Integer sortOrder) {
        // 校验知识库存在且属于当前租户
        validateKnowledgeBase(tenantId, knowledgeBaseId);

        KnowledgeItem item = new KnowledgeItem();
        item.setTenantId(tenantId);
        item.setKnowledgeBaseId(knowledgeBaseId);
        item.setTitle(title);
        item.setQuestion(question);
        item.setAnswer(answer);
        item.setProductType(productType);
        item.setFaultType(faultType);
        item.setKeywords(keywords);
        item.setSortOrder(sortOrder != null ? sortOrder : 0);
        item.setStatus(KnowledgeStatus.ACTIVE.name());
        knowledgeItemMapper.insert(item);

        // 同步到向量库（fire-and-forget）
        syncToVectorStore(tenantId, item);

        return item;
    }

    public Page<KnowledgeItem> list(Long tenantId, int page, int size,
                                    Long knowledgeBaseId, String keyword) {
        Page<KnowledgeItem> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<KnowledgeItem> wrapper = new LambdaQueryWrapper<KnowledgeItem>()
                .eq(KnowledgeItem::getTenantId, tenantId);
        if (knowledgeBaseId != null) {
            wrapper.eq(KnowledgeItem::getKnowledgeBaseId, knowledgeBaseId);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(KnowledgeItem::getTitle, keyword)
                    .or().like(KnowledgeItem::getQuestion, keyword)
                    .or().like(KnowledgeItem::getKeywords, keyword));
        }
        wrapper.orderByAsc(KnowledgeItem::getSortOrder)
                .orderByDesc(KnowledgeItem::getCreatedAt);
        return knowledgeItemMapper.selectPage(pageParam, wrapper);
    }

    public KnowledgeItem getById(Long tenantId, Long id) {
        KnowledgeItem item = knowledgeItemMapper.selectOne(
                new LambdaQueryWrapper<KnowledgeItem>()
                        .eq(KnowledgeItem::getTenantId, tenantId)
                        .eq(KnowledgeItem::getId, id)
        );
        if (item == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "知识条目不存在");
        }
        return item;
    }

    public void update(Long tenantId, Long id, Long knowledgeBaseId, String title,
                       String question, String answer, String productType,
                       String faultType, String keywords, Integer sortOrder) {
        KnowledgeItem item = getById(tenantId, id);
        if (knowledgeBaseId != null) {
            validateKnowledgeBase(tenantId, knowledgeBaseId);
            item.setKnowledgeBaseId(knowledgeBaseId);
        }
        if (title != null) item.setTitle(title);
        if (question != null) item.setQuestion(question);
        if (answer != null) item.setAnswer(answer);
        if (productType != null) item.setProductType(productType);
        if (faultType != null) item.setFaultType(faultType);
        if (keywords != null) item.setKeywords(keywords);
        if (sortOrder != null) item.setSortOrder(sortOrder);
        knowledgeItemMapper.updateById(item);

        // 同步到向量库（fire-and-forget）
        syncToVectorStore(tenantId, item);
    }

    public void updateStatus(Long tenantId, Long id, String status) {
        KnowledgeStatus ks = KnowledgeStatus.parse(status);
        KnowledgeItem item = getById(tenantId, id);
        item.setStatus(ks.name());
        knowledgeItemMapper.updateById(item);

        // 同步到向量库（fire-and-forget）
        syncToVectorStore(tenantId, item);
    }

    /**
     * 获取当前租户所有 ACTIVE 知识条目（供批量同步使用）。
     */
    public List<KnowledgeItem> listAllActive(Long tenantId) {
        return knowledgeItemMapper.selectList(
                new LambdaQueryWrapper<KnowledgeItem>()
                        .eq(KnowledgeItem::getTenantId, tenantId)
                        .eq(KnowledgeItem::getStatus, KnowledgeStatus.ACTIVE.name())
        );
    }

    // ---------- 内部方法 ----------

    private void syncToVectorStore(Long tenantId, KnowledgeItem item) {
        try {
            aiClient.syncKnowledgeItem(
                    tenantId, item.getId(), item.getKnowledgeBaseId(),
                    item.getTitle(), item.getQuestion(), item.getAnswer(),
                    item.getProductType(), item.getFaultType(), item.getStatus());
        } catch (Exception e) {
            log.warn("Failed to sync knowledge item {} to vector store: {}", item.getId(), e.getMessage());
        }
    }

    private void validateKnowledgeBase(Long tenantId, Long knowledgeBaseId) {
        KnowledgeBase kb = knowledgeBaseMapper.selectOne(
                new LambdaQueryWrapper<KnowledgeBase>()
                        .eq(KnowledgeBase::getTenantId, tenantId)
                        .eq(KnowledgeBase::getId, knowledgeBaseId)
        );
        if (kb == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "知识库不存在");
        }
    }
}
