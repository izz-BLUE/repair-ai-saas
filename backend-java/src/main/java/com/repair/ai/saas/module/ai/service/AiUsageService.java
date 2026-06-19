package com.repair.ai.saas.module.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.repair.ai.saas.common.BusinessException;
import com.repair.ai.saas.common.ResultCode;
import com.repair.ai.saas.module.ai.entity.AiUsageDaily;
import com.repair.ai.saas.module.ai.mapper.AiUsageDailyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * AI 日调用量统计服务。
 * 轻量实现：每次调用 +1，按 (tenantId, usageDate) 唯一键 upsert。
 */
@Service
@RequiredArgsConstructor
public class AiUsageService {

    private final AiUsageDailyMapper usageMapper;

    /**
     * 检查并递增调用次数。
     * 如果 maxDailyCalls 为 null 表示不限制。
     * 达上限时抛出 BusinessException。
     */
    public void checkAndIncrement(Long tenantId, Integer maxDailyCalls) {
        if (maxDailyCalls == null) {
            // 不限制，仅记录
            increment(tenantId);
            return;
        }

        LocalDate today = LocalDate.now();
        AiUsageDaily usage = usageMapper.selectOne(
                new LambdaQueryWrapper<AiUsageDaily>()
                        .eq(AiUsageDaily::getTenantId, tenantId)
                        .eq(AiUsageDaily::getUsageDate, today)
        );

        if (usage == null) {
            // 今天首次调用
            usage = new AiUsageDaily();
            usage.setTenantId(tenantId);
            usage.setUsageDate(today);
            usage.setCallCount(1);
            usageMapper.insert(usage);
            return;
        }

        if (usage.getCallCount() >= maxDailyCalls) {
            throw new BusinessException(ResultCode.FORBIDDEN,
                    "今日 AI 调用次数已达上限（" + maxDailyCalls + " 次），请明天再试");
        }

        usage.setCallCount(usage.getCallCount() + 1);
        usageMapper.updateById(usage);
    }

    /** 仅记录调用，不限制（用于内部管理后台 AI 对话） */
    private void increment(Long tenantId) {
        LocalDate today = LocalDate.now();
        AiUsageDaily usage = usageMapper.selectOne(
                new LambdaQueryWrapper<AiUsageDaily>()
                        .eq(AiUsageDaily::getTenantId, tenantId)
                        .eq(AiUsageDaily::getUsageDate, today)
        );
        if (usage == null) {
            usage = new AiUsageDaily();
            usage.setTenantId(tenantId);
            usage.setUsageDate(today);
            usage.setCallCount(1);
            usageMapper.insert(usage);
        } else {
            usage.setCallCount(usage.getCallCount() + 1);
            usageMapper.updateById(usage);
        }
    }
}
