# Redis 限流设计文档

> 创建日期：2026-06-22
> 状态：📋 方案设计，暂不实现代码

## 1. 需求

以下四个接口需要限流保护：

| 接口 | 端点 | 风险 |
|------|------|------|
| 登录 | `POST /api/public/login` | 爆破 |
| 公开报修 | `POST /api/public/{tenantCode}/repair-requests` | 批量刷单 |
| 工单查询 | `GET /api/public/{tenantCode}/tickets/query` | 爬虫枚举 |
| AI 聊天 | `POST /api/public/{tenantCode}/ai/chat` | 耗尽 AI 限额 |

## 2. 设计原则

- **尽量薄**：不引入 Redisson / Sentinel，直接用 Spring Data Redis + 自旋 Lua 脚本
- **租户级限流为主**：`tenantCode` 作为 key 前缀，避免一个租户刷单影响其他租户
- **IP 级限流为辅**：公开接口同时按 IP 限流，避免单个客户端绕过
- **滑动窗口**：使用 Redis Sorted Set + 时间戳实现（比固定窗口更公平）

## 3. Redis 数据结构

### 租户级限流
```
Key:     ratelimit:{endpoint}:{tenantCode}
Value:   ZSET，member=requestId，score=timestampMs
TTL:     windowSize * 2 (自动过期)
```

### IP 级限流
```
Key:     ratelimit:ip:{endpoint}:{clientIp}
Value:   ZSET，member=requestId，score=timestampMs
TTL:     windowSize * 2
```

## 4. 限流参数

| 接口 | 限流维度 | 窗口 | 限制 | 说明 |
|------|---------|------|------|------|
| 登录 | IP | 1 min | 5 次 | 防止爆破 |
| 报修 | IP + 租户 | 1 min | IP: 3 次, 租户: 10 次 | 防刷单 |
| 查询 | IP + 租户 | 1 min | IP: 10 次, 租户: 30 次 | 防爬虫 |
| AI 聊天 | 租户 | 1 min | 按租户 daily limit 比例折算 | 防耗尽 |

## 5. Java 实现骨架

```java
@Component
public class RateLimiter {
    private final StringRedisTemplate redis;

    public boolean isAllowed(String key, int maxCount, long windowMs) {
        long now = System.currentTimeMillis();
        long windowStart = now - windowMs;
        String redisKey = "ratelimit:" + key;

        // Lua 脚本：移除窗口外记录 → 统计当前窗口请求数 → 添加当前请求 → 设置 TTL
        String lua = """
            local key = KEYS[1]
            local now = tonumber(ARGV[1])
            local windowStart = tonumber(ARGV[2])
            local maxCount = tonumber(ARGV[3])
            local requestId = ARGV[4]
            redis.call('ZREMRANGEBYSCORE', key, 0, windowStart)
            local count = redis.call('ZCARD', key)
            if count >= maxCount then
                return 0
            end
            redis.call('ZADD', key, now, requestId)
            redis.call('EXPIRE', key, 120)
            return 1
        """;
        Long result = redis.execute(
            new DefaultRedisScript<>(lua, Long.class),
            List.of(redisKey),
            String.valueOf(now),
            String.valueOf(windowStart),
            String.valueOf(maxCount),
            UUID.randomUUID().toString()
        );
        return result != null && result == 1;
    }
}
```

## 6. 集成方式

在 Controller 入口处调用：

```java
@PostMapping("/api/public/{tenantCode}/repair-requests")
public ApiResponse<?> publicRepair(@PathVariable String tenantCode, ...) {
    // 租户级限流
    if (!rateLimiter.isAllowed("repair:" + tenantCode, 10, 60_000)) {
        throw new BusinessException(429, "请求过于频繁，请稍后重试");
    }
    // IP 级限流
    String ip = request.getRemoteAddr();
    if (!rateLimiter.isAllowed("ip:repair:" + ip, 3, 60_000)) {
        throw new BusinessException(429, "请求过于频繁，请稍后重试");
    }
    // ... 原有逻辑
}
```

## 7. 优先级

| 优先级 | 内容 | 建议版本 |
|--------|------|---------|
| P0 | 登录 IP 限流（爆破最常见） | V0.5.3 |
| P1 | 报修 IP + 租户限流 | V0.5.3 |
| P2 | 查询限流 | V0.5.4 |
| P3 | AI 聊天限流（已有 daily limit，非最紧急） | V0.5.4 |

> 当前阶段暂不实现代码，仅保留设计方案。V0.5.3 优先实现登录 IP 限流。
