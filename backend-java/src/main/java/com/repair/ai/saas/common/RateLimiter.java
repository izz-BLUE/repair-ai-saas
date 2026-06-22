package com.repair.ai.saas.common;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Redis 滑动窗口限流器。
 * 使用 ZSET + Lua 脚本实现，比固定窗口更公平。
 *
 * 设计原则：
 * - Redis 异常时 fail-open（放行），不因为 Redis 故障导致业务不可用
 * - 阈值集中定义为常量，Controller 不允许写魔法数字
 * - member 使用 now + nanoTime + sequence 避免极端冲突
 */
@Slf4j
@Component
public class RateLimiter {

    private final StringRedisTemplate redis;

    /** 用于生成唯一 member 的序列号 */
    private final AtomicLong sequence = new AtomicLong(0);

    // ==================== 限流阈值常量 ====================

    /** 登录接口：同一 IP 每分钟最多 5 次 */
    public static final int LOGIN_IP_MAX = 5;
    public static final long LOGIN_IP_WINDOW_MS = 60_000;

    /** 报修接口：同一 IP 每分钟最多 3 次 */
    public static final int REPAIR_IP_MAX = 3;
    /** 报修接口：同一租户每分钟最多 10 次 */
    public static final int REPAIR_TENANT_MAX = 10;
    public static final long REPAIR_WINDOW_MS = 60_000;

    /** 工单查询接口：同一 IP 每分钟最多 10 次 */
    public static final int QUERY_IP_MAX = 10;
    /** 工单查询接口：同一租户每分钟最多 30 次 */
    public static final int QUERY_TENANT_MAX = 30;
    public static final long QUERY_WINDOW_MS = 60_000;

    /** AI 聊天接口：同一 IP 每分钟最多 5 次（租户日限额由 AiUsageService 负责） */
    public static final int CHAT_IP_MAX = 5;
    public static final long CHAT_WINDOW_MS = 60_000;

    /** 429 响应信息（不暴露具体阈值或维度） */
    public static final String RATE_LIMIT_MSG = "请求过于频繁，请稍后重试";

    // ==================== Redis Key 前缀 ====================

    static final String PREFIX_LOGIN_IP = "ratelimit:login:ip:";
    static final String PREFIX_REPAIR_TENANT = "ratelimit:repair:tenant:";
    static final String PREFIX_REPAIR_IP = "ratelimit:repair:ip:";
    static final String PREFIX_QUERY_TENANT = "ratelimit:query:tenant:";
    static final String PREFIX_QUERY_IP = "ratelimit:query:ip:";
    static final String PREFIX_CHAT_IP = "ratelimit:chat:ip:";

    public RateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    // ==================== 公共方法 ====================

    /**
     * 检查是否允许请求。
     *
     * @param key        Redis key（不含前缀，由调用方拼接）
     * @param maxCount   窗口内最大请求数
     * @param windowMs   窗口大小（毫秒）
     * @return true=允许, false=超限拒绝
     */
    public boolean isAllowed(String key, int maxCount, long windowMs) {
        long now = System.currentTimeMillis();
        long windowStart = now - windowMs;
        String redisKey = "ratelimit:" + key;
        String member = now + ":" + System.nanoTime() + ":" + sequence.incrementAndGet();
        long ttlSeconds = Math.max(1, windowMs * 2 / 1000);

        String lua = """
            local key = KEYS[1]
            local now = tonumber(ARGV[1])
            local windowStart = tonumber(ARGV[2])
            local maxCount = tonumber(ARGV[3])
            local member = ARGV[4]
            local ttl = tonumber(ARGV[5])
            redis.call('ZREMRANGEBYSCORE', key, 0, windowStart)
            local count = redis.call('ZCARD', key)
            if count >= maxCount then
                return 0
            end
            redis.call('ZADD', key, now, member)
            redis.call('EXPIRE', key, ttl)
            return 1
            """;

        try {
            DefaultRedisScript<Long> script = new DefaultRedisScript<>(lua, Long.class);
            Long result = redis.execute(script,
                    List.of(redisKey),
                    String.valueOf(now),
                    String.valueOf(windowStart),
                    String.valueOf(maxCount),
                    member,
                    String.valueOf(ttlSeconds));
            boolean allowed = result != null && result == 1;
            if (!allowed) {
                log.warn("Rate limit triggered: key={}, maxCount={}, windowMs={}", redisKey, maxCount, windowMs);
            }
            return allowed;
        } catch (Exception e) {
            // Redis 异常时 fail-open：放行请求，不能因为 Redis 故障导致业务不可用
            log.error("Redis error during rate limit check, failing open: key={}", redisKey, e);
            return true;
        }
    }

    // ==================== 登录限流 ====================

    public boolean checkLogin(String ip) {
        return isAllowed(PREFIX_LOGIN_IP + ip, LOGIN_IP_MAX, LOGIN_IP_WINDOW_MS);
    }

    // ==================== 报修限流 ====================

    public boolean checkRepairIp(String ip) {
        return isAllowed(PREFIX_REPAIR_IP + ip, REPAIR_IP_MAX, REPAIR_WINDOW_MS);
    }

    public boolean checkRepairTenant(String tenantCode) {
        return isAllowed(PREFIX_REPAIR_TENANT + tenantCode, REPAIR_TENANT_MAX, REPAIR_WINDOW_MS);
    }

    // ==================== 工单查询限流 ====================

    public boolean checkQueryIp(String ip) {
        return isAllowed(PREFIX_QUERY_IP + ip, QUERY_IP_MAX, QUERY_WINDOW_MS);
    }

    public boolean checkQueryTenant(String tenantCode) {
        return isAllowed(PREFIX_QUERY_TENANT + tenantCode, QUERY_TENANT_MAX, QUERY_WINDOW_MS);
    }

    // ==================== AI 聊天限流 ====================

    public boolean checkChatIp(String ip) {
        return isAllowed(PREFIX_CHAT_IP + ip, CHAT_IP_MAX, CHAT_WINDOW_MS);
    }

    // ==================== IP 获取工具 ====================

    /**
     * 从 HttpServletRequest 获取客户端真实 IP。
     *
     * 优先级：X-Forwarded-For 第一个 IP → X-Real-IP → remoteAddr
     * 过滤 unknown、空字符串、0:0:0:0:0:0:0:1（IPv6 localhost）。
     *
     * @param request HTTP 请求
     * @return 客户端 IP，不会返回 null
     */
    public static String getClientIp(HttpServletRequest request) {
        String ip = null;

        // 1. X-Forwarded-For（取第一个非 unknown 的 IP）
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String[] parts = forwarded.split(",");
            for (String part : parts) {
                String candidate = part.trim();
                if (!candidate.isEmpty() && !"unknown".equalsIgnoreCase(candidate)) {
                    ip = candidate;
                    break;
                }
            }
        }

        // 2. X-Real-IP
        if (ip == null) {
            String realIp = request.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isBlank() && !"unknown".equalsIgnoreCase(realIp)) {
                ip = realIp.trim();
            }
        }

        // 3. remoteAddr
        if (ip == null) {
            ip = request.getRemoteAddr();
        }

        // 处理 IPv6 localhost → IPv4
        if ("0:0:0:0:0:0:0:1".equals(ip)) {
            ip = "127.0.0.1";
        }

        return ip != null ? ip : "unknown";
    }
}
