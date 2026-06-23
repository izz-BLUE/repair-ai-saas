package com.repair.ai.saas.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 限流器单元测试。
 * 使用 Spring MockHttpServletRequest + Stub Redis。
 * 覆盖：常量定义、getClientIp(IP 链回退)、isAllowed(允许/拒绝/fail-open)、消息不泄露。
 */
class RateLimiterTest {

    // ==================== ResultCode 429 映射 ====================

    @Test
    @DisplayName("ResultCode.TOO_MANY_REQUESTS 应存在且值为 TOO_MANY_REQUESTS")
    void resultCode_hasTooManyRequests() {
        assertEquals("TOO_MANY_REQUESTS", ResultCode.TOO_MANY_REQUESTS);
    }

    @Test
    @DisplayName("HTTP 429 对应 HttpStatus.TOO_MANY_REQUESTS")
    void http429_mapsToTooManyRequests() {
        assertEquals(429, org.springframework.http.HttpStatus.TOO_MANY_REQUESTS.value());
    }

    // ==================== RateLimiter 常量 ====================

    @Test
    @DisplayName("登录限流：IP 每分钟 5 次")
    void loginConstants() {
        assertEquals(5, RateLimiter.LOGIN_IP_MAX);
        assertEquals(60_000, RateLimiter.LOGIN_IP_WINDOW_MS);
    }

    @Test
    @DisplayName("报修限流：IP 3次/min，租户 10次/min")
    void repairConstants() {
        assertEquals(3, RateLimiter.REPAIR_IP_MAX);
        assertEquals(10, RateLimiter.REPAIR_TENANT_MAX);
        assertEquals(60_000, RateLimiter.REPAIR_WINDOW_MS);
    }

    @Test
    @DisplayName("查询限流：IP 10次/min，租户 30次/min")
    void queryConstants() {
        assertEquals(10, RateLimiter.QUERY_IP_MAX);
        assertEquals(30, RateLimiter.QUERY_TENANT_MAX);
        assertEquals(60_000, RateLimiter.QUERY_WINDOW_MS);
    }

    @Test
    @DisplayName("AI聊天限流：IP 5次/min")
    void chatConstants() {
        assertEquals(5, RateLimiter.CHAT_IP_MAX);
        assertEquals(60_000, RateLimiter.CHAT_WINDOW_MS);
    }

    // ==================== getClientIp ====================

    @Test
    @DisplayName("getClientIp: 取 X-Forwarded-For 最后一个 IP（防伪造，Nginx 追加真实 IP 在末尾）")
    void getClientIp_xForwardedFor_last() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-Forwarded-For", "evil-ip, 10.0.0.1, 172.16.0.1");
        req.setRemoteAddr("127.0.0.1");
        assertEquals("172.16.0.1", RateLimiter.getClientIp(req));
    }

    @Test
    @DisplayName("getClientIp: X-Forwarded-For 中跳过 unknown（从右向左遍历）")
    void getClientIp_xForwardedFor_skipUnknown() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-Forwarded-For", "10.0.0.1, unknown");
        req.setRemoteAddr("127.0.0.1");
        assertEquals("10.0.0.1", RateLimiter.getClientIp(req));
    }

    @Test
    @DisplayName("getClientIp: X-Forwarded-For 全是 unknown 时回退到 X-Real-IP")
    void getClientIp_xForwardedFor_allUnknown_fallbackToXRealIp() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-Forwarded-For", "unknown");
        req.addHeader("X-Real-IP", "10.0.0.2");
        req.setRemoteAddr("127.0.0.1");
        assertEquals("10.0.0.2", RateLimiter.getClientIp(req));
    }

    @Test
    @DisplayName("getClientIp: X-Real-IP 优先级高于 remoteAddr")
    void getClientIp_xRealIp() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-Real-IP", "10.0.0.5");
        req.setRemoteAddr("192.168.1.1");
        assertEquals("10.0.0.5", RateLimiter.getClientIp(req));
    }

    @Test
    @DisplayName("getClientIp: X-Real-IP 为 unknown 时回退到 remoteAddr")
    void getClientIp_xRealIp_unknown_fallbackToRemoteAddr() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-Real-IP", "unknown");
        req.setRemoteAddr("10.0.0.99");
        assertEquals("10.0.0.99", RateLimiter.getClientIp(req));
    }

    @Test
    @DisplayName("getClientIp: 无代理头时使用 remoteAddr")
    void getClientIp_remoteAddr() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("203.0.113.5");
        assertEquals("203.0.113.5", RateLimiter.getClientIp(req));
    }

    @Test
    @DisplayName("getClientIp: X-Forwarded-For 空字符串时回退")
    void getClientIp_xForwardedFor_empty() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-Forwarded-For", "");
        req.setRemoteAddr("1.2.3.4");
        assertEquals("1.2.3.4", RateLimiter.getClientIp(req));
    }

    @Test
    @DisplayName("getClientIp: X-Forwarded-For 纯空格时回退")
    void getClientIp_xForwardedFor_blank() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-Forwarded-For", "   ");
        req.setRemoteAddr("1.2.3.4");
        assertEquals("1.2.3.4", RateLimiter.getClientIp(req));
    }

    @Test
    @DisplayName("getClientIp: IPv6 localhost 转为 IPv4")
    void getClientIp_ipv6Localhost_convertsToIpv4() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("0:0:0:0:0:0:0:1");
        assertEquals("127.0.0.1", RateLimiter.getClientIp(req));
    }

    // ==================== isAllowed: Redis 返回 1（允许） ====================

    @Test
    @DisplayName("isAllowed: Redis 返回 1 时允许")
    void isAllowed_redisReturns1_allowed() {
        StringRedisTemplate redis = new StubStringRedisTemplate(1L);
        RateLimiter limiter = new RateLimiter(redis);
        assertTrue(limiter.isAllowed("test:key", 5, 60_000));
    }

    // ==================== isAllowed: Redis 返回 0（拒绝） ====================

    @Test
    @DisplayName("isAllowed: Redis 返回 0 时拒绝")
    void isAllowed_redisReturns0_denied() {
        StringRedisTemplate redis = new StubStringRedisTemplate(0L);
        RateLimiter limiter = new RateLimiter(redis);
        assertFalse(limiter.isAllowed("test:key", 5, 60_000));
    }

    // ==================== isAllowed: Redis 异常时 fail-open ====================

    @Test
    @DisplayName("isAllowed: Redis 异常时 fail-open 放行")
    void isAllowed_redisException_failOpen() {
        StringRedisTemplate redis = new StubStringRedisTemplate(
                new RuntimeException("Connection refused"));
        RateLimiter limiter = new RateLimiter(redis);
        assertTrue(limiter.isAllowed("test:key", 5, 60_000),
                "Redis 故障时必须 fail-open，不能阻止业务请求");
    }

    @Test
    @DisplayName("isAllowed: Redis exec 返回 null 时 fail-open 放行")
    void isAllowed_redisReturnsNull_failOpen() {
        StringRedisTemplate redis = new StubStringRedisTemplate((Long) null);
        RateLimiter limiter = new RateLimiter(redis);
        assertTrue(limiter.isAllowed("test:key", 5, 60_000),
                "Redis 返回 null 时必须 fail-open");
    }

    // ==================== 限流消息不暴露细节 ====================

    @Test
    @DisplayName("限流消息不应暴露具体阈值或维度")
    void rateLimitMessage_noLeak() {
        String msg = RateLimiter.RATE_LIMIT_MSG;
        assertNotNull(msg);
        assertFalse(msg.contains("5"), "不能泄露具体阈值");
        assertFalse(msg.contains("IP"), "不能泄露限流维度");
        assertFalse(msg.contains("租户"), "不能泄露限流维度");
    }

    // ==================== 辅助类 ====================

    /**
     * Redis Stub：通过构造参数控制返回 Long 值或抛异常。
     * 适配 RateLimiter 的 SessionCallback 调用方式。
     */
    private static class StubStringRedisTemplate extends StringRedisTemplate {
        private final Long returnValue;
        private final RuntimeException exception;

        StubStringRedisTemplate(Long returnValue) {
            this.returnValue = returnValue;
            this.exception = null;
        }

        StubStringRedisTemplate(RuntimeException exception) {
            this.returnValue = null;
            this.exception = exception;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T execute(org.springframework.data.redis.core.SessionCallback<T> session) {
            if (exception != null) {
                throw exception;
            }
            if (returnValue == null) {
                return null;
            }
            if (returnValue == 1L) {
                // 模拟 count(0) < max → 允许
                return (T) List.of(0L, 0L);
            }
            // returnValue == 0L: 模拟 count(5) >= max(5) → 拒绝
            return (T) List.of(0L, 5L);
        }
    }
}
