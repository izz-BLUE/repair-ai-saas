package com.repair.ai.saas.module.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.repair.ai.saas.common.BusinessException;
import com.repair.ai.saas.common.ResultCode;
import com.repair.ai.saas.module.ai.entity.AiUsageDaily;
import com.repair.ai.saas.module.ai.mapper.AiUsageDailyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * AI 日调用量统计服务。
 * 轻量实现：每次调用 +1，按 (tenantId, usageDate) 唯一键 upsert。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiUsageService {

    private final AiUsageDailyMapper usageMapper;

    /**
     * 检查并递增调用次数。
     * 如果 maxDailyCalls 为 null 表示不限制。
     * 达上限时抛出 BusinessException。
     */
    @Transactional
    public void checkAndIncrement(Long tenantId, Integer maxDailyCalls) {
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
            log.info("AI usage: tenantId={}, date={}, callCount=1 (new)", tenantId, today);
            return;
        }

        // 不限制时仅递增
        if (maxDailyCalls == null) {
            usage.setCallCount(usage.getCallCount() + 1);
            usageMapper.updateById(usage);
            log.info("AI usage: tenantId={}, date={}, callCount={} (unlimited)", tenantId, today, usage.getCallCount());
            return;
        }

        // 有限额时检查
        if (usage.getCallCount() >= maxDailyCalls) {
            log.warn("AI usage limit reached: tenantId={}, callCount={}, maxDailyCalls={}", tenantId, usage.getCallCount(), maxDailyCalls);
            throw new BusinessException(ResultCode.FORBIDDEN,
                    "今日 AI 调用次数已达上限（" + maxDailyCalls + " 次），请明天再试");
        }

        // 使用 UpdateWrapper 保证原子递增
        usageMapper.update(null, new LambdaUpdateWrapper<AiUsageDaily>()
                .eq(AiUsageDaily::getId, usage.getId())
                .set(AiUsageDaily::getCallCount, usage.getCallCount() + 1));
        log.info("AI usage: tenantId={}, date={}, callCount={} (limited, max={})", tenantId, today, usage.getCallCount() + 1, maxDailyCalls);
    }
}
