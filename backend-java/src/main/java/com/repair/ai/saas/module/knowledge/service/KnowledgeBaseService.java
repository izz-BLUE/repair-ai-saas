package com.repair.ai.saas.module.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.repair.ai.saas.common.BusinessException;
import com.repair.ai.saas.common.ResultCode;
import com.repair.ai.saas.module.knowledge.entity.KnowledgeBase;
import com.repair.ai.saas.module.knowledge.enums.KnowledgeStatus;
import com.repair.ai.saas.module.knowledge.mapper.KnowledgeBaseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

    private final KnowledgeBaseMapper knowledgeBaseMapper;

    public KnowledgeBase create(Long tenantId, String name, String description) {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setTenantId(tenantId);
        kb.setName(name);
        kb.setDescription(description);
        kb.setStatus(KnowledgeStatus.ACTIVE.name());
        knowledgeBaseMapper.insert(kb);
        return kb;
    }

    public Page<KnowledgeBase> list(Long tenantId, int page, int size, String keyword) {
        Page<KnowledgeBase> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<KnowledgeBase>()
                .eq(KnowledgeBase::getTenantId, tenantId);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(KnowledgeBase::getName, keyword)
                    .or().like(KnowledgeBase::getDescription, keyword));
        }
        wrapper.orderByDesc(KnowledgeBase::getCreatedAt);
        return knowledgeBaseMapper.selectPage(pageParam, wrapper);
    }

    public KnowledgeBase getById(Long tenantId, Long id) {
        KnowledgeBase kb = knowledgeBaseMapper.selectOne(
                new LambdaQueryWrapper<KnowledgeBase>()
                        .eq(KnowledgeBase::getTenantId, tenantId)
                        .eq(KnowledgeBase::getId, id)
        );
        if (kb == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "知识库不存在");
        }
        return kb;
    }

    public void update(Long tenantId, Long id, String name, String description) {
        KnowledgeBase kb = getById(tenantId, id);
        if (name != null) kb.setName(name);
        if (description != null) kb.setDescription(description);
        knowledgeBaseMapper.updateById(kb);
    }

    public void updateStatus(Long tenantId, Long id, String status) {
        KnowledgeStatus ks = KnowledgeStatus.parse(status);
        KnowledgeBase kb = getById(tenantId, id);
        kb.setStatus(ks.name());
        knowledgeBaseMapper.updateById(kb);
    }
}
